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
import javax.servlet.http.HttpServletRequest;
import org.raven.net.impl.RequestImpl;

/**
 *
 * @author Mikhail Titov
 */
public class CometRequestImpl extends RequestImpl implements CometRequest {
    private final AtomicReference<CometInputStream> inputStream = new AtomicReference<CometInputStream>();
    private final AtomicReference<CometReader> reader = new AtomicReference<CometReader>();
    private final AtomicBoolean requestStreamClosed = new AtomicBoolean();
    private final NetworkResponseServlet.RequestContext requestContext;
    
    public CometRequestImpl(String remoteAddr, Map<String, Object> params, Map<String, Object> headers, 
            String contextPath, String method, HttpServletRequest httpRequest) 
    {
        super(remoteAddr, params, headers, contextPath, method, httpRequest);
        this.requestContext = (NetworkResponseServlet.RequestContext) httpRequest.getAttribute(
                CometNetworkResponseServlet.REQUEST_CONTEXT);
    }

    @Override
    public InputStream getContent() throws IOException {
        if (inputStream.get()==null)
            if (inputStream.compareAndSet(null, new CometInputStream())) {
                requestContext.incomingDataListener = inputStream.get();
                if (requestStreamClosed.get())
                    inputStream.get().dataStreamClosed();
            }
        return inputStream.get();
    }

    @Override
    public Reader getContentReader() throws IOException {
        if (reader.get()==null)
            if (reader.compareAndSet(null, new CometReader(super.getContentReader())) && requestStreamClosed.get())
                reader.get().dataStreamClosed();
        return reader.get();
    }
}
