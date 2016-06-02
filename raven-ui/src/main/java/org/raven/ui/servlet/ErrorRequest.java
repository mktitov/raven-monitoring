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
import java.util.List;
import java.util.Map;
import org.raven.net.Request;
import org.raven.sched.ExecutorService;

/**
 *
 * @author Mikhail Titov
 */
public class ErrorRequest implements Request {
    private final Request request;
    private final String contextPath;
    private final String servicePath;

    public ErrorRequest(Request request, String contextPath, String servicePath) {
        this.request = request;
        this.contextPath = contextPath;
        this.servicePath = servicePath;
    }

    @Override
    public ExecutorService getExecutor() {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public int getRemotePort() {
        return request.getRemotePort();
    }

    @Override
    public String getServerHost() {
        return request.getServerHost();
    }

    @Override
    public Map<String, Object> getHeaders() {
        return request.getHeaders();
    }

    @Override
    public Map<String, Object> getParams() {
        return request.getParams();
    }

    public Map<String, Object> getAttrs() {
        return request.getAttrs();
    }

    @Override
    public String getServicePath() {
        return servicePath;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getRootPath() {
        return request.getRootPath();
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public long getIfModifiedSince() {
        return request.getIfModifiedSince();
    }

    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public InputStream getContent() throws IOException {
        return request.getContent();
    }

    @Override
    public Reader getContentReader() throws IOException {
        return request.getContentReader();
    }

    @Override
    public Map<String, List> getAllParams() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getContentCharset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
     
}
