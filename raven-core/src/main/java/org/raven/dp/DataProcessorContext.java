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
package org.raven.dp;

import java.util.Collection;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov
 */
public interface DataProcessorContext {
    public Node getOwner();
    public ExecutorService getExecutor();
    public DataProcessorFacade getSender();    
    public LoggerHelper getLogger();
    public Stashed stash();
    public Stashed stash(Object message);
    public void unstashAll() throws NoStashedMessagesException;
    public void become(DataProcessor dataProcessor, boolean replace);
    public void unbecome() throws UnbecomeFailureException;
    public void forward(DataProcessorFacade facade);
    public void forward(DataProcessorFacade facade, Object message);
//    public void unhandled();
    public DataProcessorFacade getParent();
    public DataProcessorFacade addChild(DataProcessorFacadeConfig config) throws NonUniqueNameException;
    public DataProcessorFacade getChild(String name);
    public Collection<DataProcessorFacade> getChildren();
    public DataProcessorFacadeConfig createChild(String name, DataProcessor processor);
}
