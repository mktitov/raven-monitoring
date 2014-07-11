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
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.net.impl.RequestImpl;
import org.raven.ui.servlet.NetworkResponseServlet.RequestContext;

/**
 *
 * @author Mikhail Titov
 */
public class CometRequestImpl extends RequestImpl implements CometRequest {
    private final RequestContext requestContext;
    private final AtomicReference<CometInputStreamImpl> inputStream = new AtomicReference<CometInputStreamImpl>();
    private final AtomicReference<CometReader> reader = new AtomicReference<CometReader>();
    private final AtomicBoolean requestStreamClosed = new AtomicBoolean();
    
    public CometRequestImpl(RequestContext requestContext, Map<String, Object> params, Map<String, Object> headers, 
            String contextPath) 
    {
        super(requestContext.request.getRemoteAddr(), params, headers, contextPath, 
                requestContext.request.getMethod().toUpperCase(), requestContext.request);
        this.requestContext = requestContext;
    }

    @Override
    public InputStream getContent() throws IOException {
        return requestContext.getRequestStream();
    }

    @Override
    public Reader getContentReader() throws IOException {
        return requestContext.getRequestReader();
    }
}
