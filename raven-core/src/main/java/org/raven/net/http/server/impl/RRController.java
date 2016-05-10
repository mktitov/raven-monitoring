/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.raven.auth.LoginService;
import org.raven.net.Request;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseServiceNode;
import org.raven.net.impl.ResponseContextImpl;
import org.raven.sched.ExecutorService;

/**
 * Request/Response controller
 * @author Mikhail Titov
 */
public class RRController {
    private final ChannelHandlerContext ctx;
    private final ExecutorService executor;

    public RRController(ChannelHandlerContext ctx, ExecutorService executor) {
        this.ctx = ctx;
        this.executor = executor;
    }
    
    public void onRequest(final HttpRequest request) {
        
    }
    
    public void onRequestContent(final HttpContent chunk) {
        
    }
    
    //free resources. For example reference counted
    public void release() {
        
    }
    
    private class RespContext extends ResponseContextImpl {

        @Override
        public OutputStream getResponseStream() throws IOException {
            return super.getResponseStream();
        }

        @Override
        public PrintWriter getResponseWriter() throws IOException {
            return super.getResponseWriter();
        }        

        public RespContext(Request request, String builderPath, String subcontext, long requestId, 
                LoginService loginService, ResponseBuilder responseBuilder, ResponseServiceNode serviceNode) 
        {
            super(request, builderPath, subcontext, requestId, loginService, responseBuilder, serviceNode);
        }
    } 
}
