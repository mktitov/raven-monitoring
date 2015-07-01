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
package org.raven.net.impl;

import org.raven.net.Result;

/**
* 
 * @author Mikhail Titov
 */
public class ResultImpl implements Result {
    private final int statusCode;
    private final Object content;
    private final String contentType;

    public ResultImpl(int statusCode, Object content) {
        this.statusCode = statusCode;
        this.content = content;
        this.contentType = null;
    }

    public ResultImpl(int statusCode, Object content, String contentType) {
        this.statusCode = statusCode;
        this.content = content;
        this.contentType = contentType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }
}
