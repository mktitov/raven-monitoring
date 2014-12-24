/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.net.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.raven.net.Request;

/**
 *
 * @author Mikhail Titov
 */
public class RequestImpl implements Request {
    private final String remoteAddr;
    private final Map<String, Object> params;
    private final Map<String, Object> headers;
    private final String contextPath;
    private final String method;
    private final String servicePath;
    private final HttpServletRequest httpRequest;
    private Map<String, Object> attrs = null;

    public RequestImpl(String remoteAddr, Map<String, Object> params, Map<String, Object> headers, 
            String contextPath, String method, HttpServletRequest httpRequest) 
    {
        this.remoteAddr = remoteAddr;
        this.params = params;
        this.headers = headers;
        this.contextPath = contextPath;
        this.method = method;
        this.httpRequest = httpRequest;
        this.servicePath = httpRequest.getServletPath().substring(1);
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public int getRemotePort() {
        return httpRequest.getRemotePort();
    }

    public String getServerHost() {
        return httpRequest.getLocalName();
    }

    public Map<String, Object> getAttrs() {
        if (attrs!=null)
            return attrs;
        else {
            synchronized(this) {
                if (attrs==null)
                    attrs = new HashMap<String, Object>();
                return attrs;
            }
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public String getServicePath() {
        return servicePath;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getMethod() {
        return method;
    }

    public String getContentType() {
        return httpRequest.getContentType();
    }

    public InputStream getContent() throws IOException {
        return httpRequest.getInputStream();
    }

    public Reader getContentReader() throws IOException {
        return httpRequest.getReader();
    }

    public String getRootPath() {
        return httpRequest.getContextPath();
    }

    public long getIfModifiedSince() {
        return httpRequest.getDateHeader("If-Modified-Since");
    }
}
