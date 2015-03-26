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

import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorContext;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.DataProcessorLogic;
import org.raven.dp.UnbecomeFailureException;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataProcessorLogic implements DataProcessorLogic {
    private DataProcessorFacade facade;
    private DataProcessorContext context;

    
    public void init(DataProcessorFacade facade, DataProcessorContext context) {
        this.facade = facade;
        this.context = context;
        postInit();
    }
    
    @Override
    public void postStop() {
    }

    public void postInit(){        
    }

    @Override
    public void childTerminated(DataProcessorFacade child) {
    }

    public DataProcessorContext getContext() {
        return context;
    }

    public DataProcessorFacade getFacade() {
        return facade;
    }
    
    protected DataProcessorFacade getSender() {
        return context.getSender();
    }
    
    protected LoggerHelper getLogger() {
        return context.getLogger();
    }
    
    protected void become(DataProcessor processor, boolean replace) {
        context.become(processor, replace);
    }
    
    protected void unbecome() throws UnbecomeFailureException {
        context.unbecome();
    }
    
    protected final Object unhandled() {
        context.unhandled();
        return VOID;
    }

    @Override
    public String toString() {
        return "MAIN";
    }
}
