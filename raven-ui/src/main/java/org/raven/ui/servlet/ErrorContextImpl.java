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

import org.raven.net.Request;

/**
 *
 * @author Mikhail Titov
 */
public class ErrorContextImpl implements ErrorContext {
    private final Request request;
    private final int statusCode;
    private final String message;
    private final Throwable error;

    public ErrorContextImpl(int statusCode, Request request, String message, Throwable error) {
        this.request = request;
        this.statusCode = statusCode;
        this.message = message;
        this.error = error;
    }

    public Request getRequest() {
        return request;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }    
}
