/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.dp.impl;

import org.raven.dp.DataProcessorFacade;
import org.raven.dp.UnhandledMessage;

/**
 *
 * @author Mikhail Titov
 */
public class UnhandledMessageImpl implements UnhandledMessage {
    private final DataProcessorFacade sender;
    private final DataProcessorFacade receiver;
    private final Object message;
    private final Throwable error;

    public UnhandledMessageImpl(DataProcessorFacade sender, DataProcessorFacade receiver, Object message) {
        this(sender, receiver, message, null);
    }
    
    public UnhandledMessageImpl(DataProcessorFacade sender, DataProcessorFacade receiver, Object message, Throwable error) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.error = error;
    }

    public DataProcessorFacade getSender() {
        return sender;
    }

    public DataProcessorFacade getReceiver() {
        return receiver;
    }

    public Throwable getError() {
        return error;
    }

    public Object getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format(
                "UNHANDLED MESSAGE from (%s) received by (%s): hasError=%s, message: %s", 
                sender==null?"UNKNOWN source":sender, receiver, error!=null, message);
    }
}
