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

import io.netty.handler.codec.http.HttpObject;

/**
 *
 * @author Mikhail Titov
 */
public class ResponseMessage<T> {
    private final RRController rrController;
    private final T message;

    protected ResponseMessage(RRController rrController, T message) {
        this.rrController = rrController;
        this.message = message;
    }


    public RRController getRrController() {
        return rrController;
    }   

    public T getMessage() {
        return message;
    }
}
