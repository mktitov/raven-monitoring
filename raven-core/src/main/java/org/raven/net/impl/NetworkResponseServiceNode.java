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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.script.Bindings;
import org.apache.commons.lang.StringUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.net.Authentication;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseContext;
import org.raven.net.NetworkResponseNode;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Response;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, childNodes={IfNode.class})
public class NetworkResponseServiceNode extends BaseNode implements NetworkResponseNode
{
    public final static String NAME = "NetworkResponseService";
    public static final String SUBCONTEXT_PARAM = "subcontext";
    public static final String REQUESTER_IP_BINDING = "requesterIp";

    @Service
    private static NetworkResponseService networkResponseService;
    public static final String REQUEST_PARAMS_BINDING = "requestParams";
    public static final String CONTEXT_BINDING = "requestContext";

    @Parameter(readOnly=true)
    private AtomicLong requestsCount;

    @Parameter(readOnly=true)
    private AtomicLong requestsWithErrors;
    
    private BindingSupportImpl bindingSupport;

    public NetworkResponseServiceNode() {
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
    protected void initFields() {
        super.initFields();
        requestsCount = new AtomicLong(0);
        requestsWithErrors = new AtomicLong(0);
        bindingSupport = new BindingSupportImpl();
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

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Authentication getAuthentication(String context, String requesterIp) 
        throws NetworkResponseServiceExeption
    {
        try {
            bindingSupport.put(CONTEXT_BINDING, context);
            bindingSupport.put(REQUESTER_IP_BINDING, requesterIp);
            return getContext(this, splitContextToPathElements(context), 0, null).getAuthentication();
        } finally {
            bindingSupport.reset();
        }
    }
    
    public Response getResponse(String context, String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        long requestId = requestsCount.incrementAndGet();
        NetworkResponseContext contextNode = null;
        if (isLogLevelEnabled(LogLevel.DEBUG)) {
            String requestInfo = String.format(
                    "[%d] Processing request. " +
                    "Remote address (%s), context (%s), request parameters: %s"
                    , requestId, requesterIp, context, paramsToString(params));
            debug(requestInfo);
        }
        try {
            try {
                bindingSupport.put(REQUESTER_IP_BINDING, requesterIp);
                bindingSupport.put(REQUEST_PARAMS_BINDING, params);
                bindingSupport.put(CONTEXT_BINDING, context);
                contextNode = getContext(this, splitContextToPathElements(context), 0, params);
            } finally {
                bindingSupport.reset();
            }

            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("[%d] Found context for request", requestId));
            if (contextNode.isLogLevelEnabled(LogLevel.DEBUG))
                contextNode.getLogger().debug(String.format(
                    "[%d] Processing request. " +
                    "Remote address (%s), context (%s), request parameters: %s"
                    , requestId, requesterIp, context, paramsToString(params)));
            Response response = contextNode.getResponse(requesterIp, params);
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
        } catch(NetworkResponseServiceExeption e) {
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
        } catch(RuntimeException e) {
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
    
    private String[] splitContextToPathElements(String context) {
        return context.split("/");
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
            buf.append(param.getKey()).append(" - (").append(param.getValue()).append(")");
            if (firstIteration)
                firstIteration = false;
        }
        return buf.toString();
    }

    private NetworkResponseContext getContext(Node node, String[] path, int pathIndex, Map<String, Object> params) 
        throws ContextUnavailableException
    {
        final Collection<Node> childs = node.getEffectiveChildrens();
        final String pathElem = path[pathIndex];
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs) {
                if (pathElem.equals(child.getName()) && Node.Status.STARTED.equals(child.getStatus())) {
                    if (child instanceof NetworkResponseGroupNode)
                        return getContext(child, path, pathIndex+1, params);
                    else {
                        if (pathIndex!=path.length-1)
                            params.put(SUBCONTEXT_PARAM, getContextFromPath(path, pathIndex+1));
                        return (NetworkResponseContext) child;
                    }
                }
            }
        throw new ContextUnavailableException(String.format(
            "Can't resolve path element (%s) for path (%s) ", pathElem, getContextFromPath(path, 0)));
    }
    
    private String getContextFromPath(String[] path, int fromIndex) {
        return StringUtils.join(path, "/", fromIndex, path.length);
    }
    
//    private NetworkResponseContext getContext(String context, Map<String, Object> params)
//            throws ContextUnavailableException
//    {
//        int pos = context.indexOf('/');
//        if (pos>=0) {
//            String subcontext = context.substring(pos+1);
//            context = context.substring(0, pos);
//            if (params!=null)
//                params.put(SUBCONTEXT_PARAM, subcontext);
//        }
//        NetworkResponseContext contextNode = (NetworkResponseContext) getChildren(context);
//        if (contextNode==null || !contextNode.getStatus().equals(Status.STARTED))
//           throw new ContextUnavailableException(context);
//        else
//            return contextNode;
//    }
}
