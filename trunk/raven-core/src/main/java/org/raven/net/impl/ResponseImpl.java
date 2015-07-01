/*
 * Copyright 2012 Mikhail Titov.
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

import java.nio.charset.Charset;
import java.util.Map;
import org.raven.net.Response;

/**
 *
 * @author Mikhail Titov
 */
public class ResponseImpl implements Response {
    private final String contentType;
    private final Object content;
    private final Map<String, String> headers;
    private final Long lastModified;
    private final Charset charset;

    public ResponseImpl(String contentType, Object content, Map<String, String> headers, Long lastModified, 
            Charset charset) 
    {
        this.contentType = contentType;
        this.content = content;
        this.headers = headers;
        this.lastModified = lastModified;
        this.charset = charset;
    }

    public String getContentType() {
        return contentType;
    }

    public Object getContent() {
        return content;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public Charset getCharset() {
        return charset;
    }

    @Override
    public String toString() {
        return content==null? "NO_CONTENT" : "HAS_CONTENT";
    }
    
}
