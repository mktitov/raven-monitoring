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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private boolean connected = false;
    private long requestSetNumber;
    private int requestNumber;
    private AtomicInteger errorsCounter;
    private AtomicInteger handledCounter;

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
        HttpResponse response = null;
        try
        {
            HttpSessionNode session = (HttpSessionNode) owner;
            requestSetNumber = session.getNextRequestSetNumber();
            requestNumber = 0;

            statusMessage.set("Starting processing data from "+dataSource.getPath());
            if (data==null){
                if (owner.isLogLevelEnabled(LogLevel.DEBUG))
                    owner.getLogger().debug(logMess("Skipping processing data because of it's NULL"));
                return null;
            }
            
            initCounters(context, session);

            if (isTooManyErrors(session))
                return HttpSessionNode.SKIP_DATA;
            handledCounter.incrementAndGet();

            Collection<Node> childs = session.getHandlers(isNewSession, data, context);
            if (childs==null)
                return null;
            Object res = null;
            requestNumber=0;
            String requestUri="";
            if (!connected && !session.getUseSessionMode())
                connected = true;
            for (Node child: childs) {
                if (   !(child instanceof HttpResponseHandlerNode) 
                    || !Status.STARTED.equals(child.getStatus()))
                {
                    continue;
                }
                HttpResponseHandlerNode handler = (HttpResponseHandlerNode) child;
                boolean isRequest = child instanceof HttpRequestNode;
                
                if (connected && isRequest && ((HttpRequestNode)handler).getSessionInitializer())
                    continue;
                
                Map<String, Object> params = initParamsAndRequest(
                        data, context, isRequest, session, response);
                logInitProcessing(isRequest, child, session);
                
                Object pres = processResponseStatusCode(
                        handler, response, session, context, params, requestUri);
                if (pres!=null)
                    return pres;

                long start = handler.operationStatistic.markOperationProcessingStart();
                try
                {
                    try{
                        res = handler.processResponse(params);
                    }finally{
                        if (response!=null)
                            response.getEntity().consumeContent();
                    }
                    ++requestNumber;
                    if (HttpSessionNode.SKIP_DATA.equals(res))
                        return HttpSessionNode.SKIP_DATA;
                    if (HttpSessionNode.STOP_PROCESSING.equals(res))
                        return data;

                    if (isRequest)
                    {
                        Map requestMap = (Map)params.get(HttpSessionNode.REQUEST);
                        HttpRequest request = (HttpRequest)requestMap.get(
                                HttpSessionNode.REQUEST_REQUEST);
                        HttpHost target = new HttpHost(
                                (String)requestMap.get(HttpSessionNode.HOST)
                                , (Integer)requestMap.get(HttpSessionNode.PORT));
                        requestUri = request.getRequestLine().getMethod() + " "
                                +request.getRequestLine().getUri();
                        if (session.isLogLevelEnabled(LogLevel.DEBUG))
                            session.getLogger().debug(logMess(
                                    "Sending request for node ("+child.getName()+"): "+requestUri));
                        try{
                            response = client.execute(target, request);
                        }catch(Throwable e){
                            if (session.isLogLevelEnabled(LogLevel.WARN))
                                session.getLogger().warn(logMess(
                                        "Executing request error. "+e.getMessage()));
                            return handleError(context, session, response==null? 
                                    null : response.getEntity(), params);
                        }
                        if (session.isLogLevelEnabled(LogLevel.DEBUG))
                            session.getLogger().debug(logMess(
                                    "Response status: "+response.getStatusLine().toString()));
                    }
                }
                finally {
                    handler.operationStatistic.markOperationProcessingEnd(start);
                }
            }
            return res;
        }
        finally {
            isNewSession = false;
        }
    }

    private void logInitProcessing(boolean isRequest, Node child, HttpSessionNode session) {
        String msg = "Processing "+(isRequest? "request" : "response")+" ("+child.getName()+")";
        statusMessage.set(msg);
        if (requestNumber>0 && session.isLogLevelEnabled(LogLevel.DEBUG))
            session.getLogger().debug(logMess("Processing response"));
    }
    
    private Map<String, Object> initParamsAndRequest(Object data, DataContext context
            , boolean isRequest, HttpSessionNode session, HttpResponse response)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(HttpSessionNode.DATA_BINDING, data);
        params.put(HttpSessionNode.DATA_CONTEXT_BINDING, context);
        params.put(HttpSessionNode.SKIP_DATA_BINDING, HttpSessionNode.SKIP_DATA);
        params.put(HttpSessionNode.STOP_PROCESSING_BINDING, HttpSessionNode.STOP_PROCESSING);
        params.put(HttpSessionNode.IS_NEW_SESSION_BINDING, isNewSession);
        if (isRequest)
            params.put(HttpSessionNode.REQUEST, session.initRequest());
        Map responseMap = new HashMap();
        responseMap.put(HttpSessionNode.RESPONSE_RESPONSE, response);
        params.put(HttpSessionNode.RESPONSE, responseMap);
        
        return params;
    }
    
    private Object processResponseStatusCode(HttpResponseHandlerNode handler, HttpResponse response
            , HttpSessionNode session, DataContext context, Map params, String requestUri)
        throws Exception
    {
        Integer expectedStatus = handler.getExpectedResponseStatusCode();
        if (response!=null && expectedStatus!=null
            && !expectedStatus.equals(response.getStatusLine().getStatusCode()))
        {
            if (session.isLogLevelEnabled(LogLevel.WARN))
                session.getLogger().warn(logMess(String.format(
                        "Invalid response status code (expected %s but was %s) for request: %s"
                        , expectedStatus, response.getStatusLine().getStatusCode(), requestUri)));
            return handleError(context, session, response.getEntity(), params);
        }
        HttpRequestNode request = 
                (HttpRequestNode)(handler instanceof HttpRequestNode? handler : null);
        if (!connected && (request==null || !request.getSessionInitializer())) {
            if (session.isLogLevelEnabled(LogLevel.DEBUG))
                session.getLogger().debug(logMess("Successfully connected"));
            connected = true;
        }
        return null;
    }

    private void initCounters(DataContext context, HttpSessionNode session)
    {
        synchronized (context) {
            String errorsCountParam = getErrorsCountParamName(session);
            errorsCounter = (AtomicInteger) context.getParameters().get(errorsCountParam);
            if (errorsCounter == null) {
                errorsCounter = new AtomicInteger(0);
                context.getParameters().put(errorsCountParam, errorsCounter);
            }
            String handledCountParam = getHandledCountParamName(session);
            handledCounter = (AtomicInteger) context.getParameters().get(handledCountParam);
            if (handledCounter == null) {
                handledCounter = new AtomicInteger(0);
                context.getParameters().put(handledCountParam, handledCounter);
            }
        }
    }

    private boolean isTooManyErrors(HttpSessionNode session)
    {
        return     handledCounter.get() > session.getCheckMaxPercentOfErrorsAfter()
                && (100.*errorsCounter.get()/handledCounter.get()) > session.getMaxPercentOfErrors();
    }

    private String logMess(String mess)
    {
        if (requestNumber==0)
            return String.format("[%s] %s", requestSetNumber, mess);
        else
            return String.format("[%s][%s] %s", requestSetNumber, requestNumber, mess);
    }

    private String getErrorsCountParamName(HttpSessionNode session){
        return ""+session.getId()+"_errorsCount";
    }

    private String getHandledCountParamName(HttpSessionNode session){
        return ""+session.getId()+"_handledCount";
    }

    private Object handleError(DataContext context, HttpSessionNode session, HttpEntity entity
            , Map<String, Object> params)
        throws IOException
    {
        if (entity!=null)
            entity.consumeContent();

        errorsCounter.incrementAndGet();
        if (isTooManyErrors(session) && session.isLogLevelEnabled(LogLevel.WARN))
            session.getLogger().warn(logMess("Too many errors. Skipping the rest of the data in the data set"));

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
