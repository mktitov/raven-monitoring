/*
 *  Copyright 2009 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.net.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseContext;
import org.raven.net.NetworkResponseNode;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.tree.impl.BaseNode;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServiceNode extends BaseNode implements NetworkResponseNode
{
    public final static String NAME = "NetworkResponseService";
    public static final String SUBCONTEXT_PARAM = "subcontext";

    @Service
    private static NetworkResponseService networkResponseService;

    @Parameter(readOnly=true)
    private AtomicLong requestsCount;

    @Parameter(readOnly=true)
    private AtomicLong requestsWithErrors;

    public NetworkResponseServiceNode()
    {
        super(NAME);
    }

    public AtomicLong getRequestsCount()
    {
        return requestsCount;
    }

    public AtomicLong getRequestsWithErrors()
    {
        return requestsWithErrors;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        requestsCount = new AtomicLong(0);
        requestsWithErrors = new AtomicLong(0);
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        networkResponseService.setNetworkResponseServiceNode(this);
        requestsCount.set(0);
        requestsWithErrors.set(0);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        networkResponseService.setNetworkResponseServiceNode(null);
    }

    public String getResponse(String context, String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        long requestId = requestsCount.incrementAndGet();
        NetworkResponseContext contextNode = null;
        if (isLogLevelEnabled(LogLevel.DEBUG))
        {
            String requestInfo = String.format(
                    "[%d] Processing request. " +
                    "Remote address (%s), context (%s), request parameters: %s"
                    , requestId, requesterIp, context, paramsToString(params));
            debug(requestInfo);
        }
        try
        {
            int pos = context.indexOf('/');
            if (pos>=0)
            {
                String subcontext = context.substring(pos+1);
                context = context.substring(0, pos);
                params.put(SUBCONTEXT_PARAM, subcontext);
            }
            contextNode = (NetworkResponseContext) getChildren(context);
            if (contextNode==null)
            {
            }
            if (contextNode==null || !contextNode.getStatus().equals(Status.STARTED))
               throw new ContextUnavailableException(context);

            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("[%d] Found context for request", requestId));
            if (contextNode.isLogLevelEnabled(LogLevel.DEBUG))
                contextNode.getLogger().debug(String.format(
                    "[%d] Processing request. " +
                    "Remote address (%s), context (%s), request parameters: %s"
                    , requestId, requesterIp, context, paramsToString(params)));
            String response = contextNode.getResponse(requesterIp, params);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("[%d] Request successfully processed", requestId));
            if (contextNode.isLogLevelEnabled(LogLevel.DEBUG))
                contextNode.getLogger().debug(
                        String.format("[%d] Request successfully processed", requestId));
            if (isLogLevelEnabled(LogLevel.TRACE))
                trace(String.format("[%d] Request response \n>>>>\n%s\n<<<<", requestId, response));
            if (contextNode.isLogLevelEnabled(LogLevel.TRACE))
                contextNode.getLogger().trace(String.format(
                        "[%d] Request response \n>>>>\n%s\n<<<<", requestId, response));
            return response;
        }
        catch(NetworkResponseServiceExeption e)
        {
            requestsWithErrors.incrementAndGet();
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage()));
            if (contextNode!=null && contextNode.isLogLevelEnabled(LogLevel.WARN))
                contextNode.getLogger().warn(String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage()));
            throw e;
        }
        catch(RuntimeException e)
        {
            requestsWithErrors.incrementAndGet();
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage())
                    , e);
            if (contextNode!=null && contextNode.isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage())
                    , e);
            throw e;
        }
    }

    private static String paramsToString(Map<String, Object> params)
    {
        if (params==null || params.isEmpty())
            return "NO PARAMETERS";
        StringBuilder buf = new StringBuilder();
        boolean firstIteration = true;
        for (Map.Entry<String, Object> param: params.entrySet())
        {
            if (!firstIteration)
                buf.append("; ");
            buf.append(param.getKey()+" - ("+param.getValue()+")");
            if (firstIteration)
                firstIteration = false;
        }
        return buf.toString();
    }
}
