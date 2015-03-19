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
package org.raven.ds.impl;

import org.raven.ds.DataProcessorFacade;
import org.raven.ds.DataProcessorLogic;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataProcessorLogic implements DataProcessorLogic {
    private DataProcessorFacade facade;
    private DataProcessorFacade sender;

    public void setFacade(DataProcessorFacade facade) {
        this.facade = facade;
        init(facade);
    }

    public DataProcessorFacade getFacade() {
        return facade;
    }

    public void setSender(DataProcessorFacade sender) {
        this.sender = sender;
    }

    public DataProcessorFacade getSender() {
        return sender;
    }
    
    protected abstract void init(DataProcessorFacade facade);

    public void postStop() {
    }
}
