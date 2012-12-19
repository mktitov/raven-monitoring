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

    public ResponseImpl(String contentType, Object content, Map<String, String> headers) {
        this.contentType = contentType;
        this.content = content;
        this.headers = headers;
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
}
