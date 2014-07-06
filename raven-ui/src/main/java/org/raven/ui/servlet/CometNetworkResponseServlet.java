/*
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.ui.servlet;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.apache.catalina.CometProcessor;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.impl.CometRequestImpl;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class CometNetworkResponseServlet extends NetworkResponseServlet implements CometProcessor {

    public void event(CometEvent ce) throws IOException, ServletException {
        final HttpServletRequest request = ce.getHttpServletRequest();
        final HttpServletResponse response = ce.getHttpServletResponse();
        switch (ce.getEventType()) {
            case BEGIN:
                initResponseProcessing(ce, request, response);
                break;
            case READ:
                break;
            case END:
                break;
            case ERROR:
                break;
        }
    }
    
    private void initResponseProcessing(CometEvent ce, HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException
    {
        final RequestContext ctx = createContext(request, response);
        try {
            final ExecutorService executor = ctx.responseService.getExecutor();
            if (executor==null)
                throw new ServletException("Comet servlet can't work without executor service");
            configureRequestContext(ctx);
            ctx.responseContext = new CometResponseContext(ctx.responseContext, ctx.response, ce);
            final Node builderNode = ctx.responseContext.getResponseBuilder().getResponseBuilderNode();
            executor.execute(new AbstractTask(builderNode, "Processing http request") {                
                @Override public void doRun() throws Exception {
                    Response result = ctx.responseContext.getResponse(ctx.user);
                    processServiceResponse(ctx, result);
                    
                }
            });
        } catch (Throwable e) {
            processError(ctx, e);
        }
    }

    @Override
    protected Request createServiceRequest(RequestContext ctx, Map<String, Object> params, 
            Map<String, Object> headers, String context) 
    {
        return new CometRequestImpl(ctx.request.getRemoteAddr(), params, headers, context, 
                    ctx.request.getMethod().toUpperCase(), ctx.request);
    }
}
