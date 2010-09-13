/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.net.http;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.raven.ds.DataContext;
import org.raven.ds.DataHandler;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class HttpSessionDataHandler implements DataHandler
{
    private final HttpClient client;
    private AtomicReference<String> statusMessage;
    private boolean isNewSession = true;

    public HttpSessionDataHandler(HttpClient client)
    {
        this.client = client;
        statusMessage = new AtomicReference<String>("http session handler created");
    }

    public void releaseHandler()
    {
    }

    public Object handleData(Object data, DataSource dataSource, DataContext context, Node owner)
            throws Exception
    {
        try
        {
            statusMessage.set("Starting processing data from "+dataSource.getPath());
            if (data==null){
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug("Skiping processing data because of it's NULL");
                return null;
            }
            HttpSessionNode session = (HttpSessionNode) owner;
            
            Integer errorsCount = (Integer) context.getParameters().get(getErrorsCountParamName(session));
            errorsCount = errorsCount==null? 0 : errorsCount;
            Integer maxErrors = session.getMaxErrorsPerDataSet();
            maxErrors = maxErrors==null? Integer.MAX_VALUE : maxErrors;
            if (errorsCount>=maxErrors)
                return HttpSessionNode.SKIP_DATA;

            Collection<Node> childs = session.getHandlers(isNewSession, data);
            Object res = null;
            if (childs!=null)
            {
                HttpResponse response = null;
                for (Node child: childs)
                {
                    if (child instanceof HttpResponseHandlerNode && Status.STARTED.equals(child.getStatus()))
                    {
                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put(HttpSessionNode.DATA_BINDING, data);
                        params.put(HttpSessionNode.DATA_CONTEXT_BINDING, context);
                        params.put(HttpSessionNode.SKIP_DATA_BINDING, HttpSessionNode.SKIP_DATA);
                        params.put(HttpSessionNode.IS_NEW_SESSION_BINDING, isNewSession);
                        HttpResponseHandlerNode handler = (HttpResponseHandlerNode) child;

                        boolean isRequest = child instanceof HttpRequestNode;
                        String msg = "Processing "+(isRequest? "request" : "response")+" ("+child.getName()+")";
                        statusMessage.set(msg);
                        if (session.isLogLevelEnabled(LogLevel.DEBUG))
                            session.getLogger().debug(msg);
                        if (isRequest)
                            params.put(HttpSessionNode.REQUEST, session.initRequest());

                        Map responseMap = new HashMap();
                        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
                        params.put(HttpSessionNode.RESPONSE, responseMap);

                        Integer expectedStatus = handler.getExpectedResponseStatusCode();
                        if (response!=null && expectedStatus!=null
                            && !expectedStatus.equals(response.getStatusLine().getStatusCode()))
                        {
                            return handleError(context, session, response.getEntity(), params, errorsCount, maxErrors);
                        }

                        long start = handler.operationStatistic.markOperationProcessingStart();
                        try
                        {
                            res = handler.processResponse(params);

                            if (isRequest)
                            {
                                Map requestMap = (Map)params.get(HttpSessionNode.REQUEST);
                                HttpRequest request = (HttpRequest)requestMap.get(HttpSessionNode.REQUEST_REQUEST);
                                HttpHost target = new HttpHost(
                                        (String)requestMap.get(HttpSessionNode.HOST)
                                        , (Integer)requestMap.get(HttpSessionNode.PORT));
                                if (session.isLogLevelEnabled(LogLevel.DEBUG))
                                    session.getLogger().debug(
                                            "Sending request: "+request.getRequestLine().getMethod()
                                            + " "+request.getRequestLine().getUri());
                                try{
                                    response = client.execute(target, request);
                                }catch(Throwable e){
                                    if (session.isLogLevelEnabled(LogLevel.DEBUG))
                                        session.getLogger().debug("Executing request error. "+e.getMessage());
                                    return handleError(
                                            context, session, response==null? null : response.getEntity()
                                            , params, errorsCount, maxErrors);
                                }
                                if (session.isLogLevelEnabled(LogLevel.DEBUG))
                                    session.getLogger().debug("Response status: "+response.getStatusLine().toString());
                            }
                        }
                        finally
                        {
                            handler.operationStatistic.markOperationProcessingEnd(start);
                        }
                    }
                }

            }
            return res;
        }
        finally
        {
            isNewSession = false;
        }
    }

    private String getErrorsCountParamName(HttpSessionNode session){
        return ""+session.getId()+"_errorsCount";
    }

    private Object handleError(DataContext context, HttpSessionNode session, HttpEntity entity
            , Map<String, Object> params, Integer errorsCount, Integer maxErrors)
        throws IOException
    {
        if (entity!=null)
            entity.consumeContent();

        ++errorsCount;
        context.getParameters().put(getErrorsCountParamName(session), errorsCount);
        if (errorsCount>=maxErrors){
            if (session.isLogLevelEnabled(LogLevel.WARN))
                session.getLogger().warn("Too many errors. Skipping the rest of the data in the data set");
        }

        return session.handleError(params);
    }

    public String getStatusMessage()
    {
        return statusMessage.get();
    }

    private Collection<Node> getHandlers(Node owner)
    {
        Collection<Node> handlers = owner.getEffectiveChildrens();

        return handlers;
    }
}
