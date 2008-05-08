/*
 *  Copyright 2008 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataSource extends ContainerNode implements DataSource
{
    @Parameter @Description("Sets the core number of threads")
    private int corePoolSize = 3;
    @Parameter @Description("Sets the maximum allowed number of threads")
    private int maximumPoolSize = 6;
    
    private ScheduledThreadPoolExecutor executorService = null;
    private Collection<NodeAttribute> consumerAttributes = new ArrayList<NodeAttribute>();

    @Override
    public void init() throws NodeError
    {
        super.init();
        
        consumerAttributes.add(
                new NodeAttributeImpl("interval", Integer.class, "the period between executions"));
        consumerAttributes.add(
                new NodeAttributeImpl(
                    "intervalUnit", String.class, "the time unit of the intervale attribute"));
        
        executorService = 
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(corePoolSize);
        executorService.setMaximumPoolSize(maximumPoolSize);
    }

    public void addDataConsumer(DataConsumer dataConsumer)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeDataConsumer(DataConsumer dataConsumer)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getCorePoolSize()
    {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize)
    {
        this.corePoolSize = corePoolSize;
        if (executorService!=null)
            executorService.setCorePoolSize(corePoolSize);
    }

    public int getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize)
    {
        this.maximumPoolSize = maximumPoolSize;
        if (executorService!=null)
            executorService.setMaximumPoolSize(maximumPoolSize);
    }
    
    private class Task implements Runnable
    {
        private final DataConsumer dataConsumer;

        public Task(DataConsumer dataConsumer)
        {
            this.dataConsumer = dataConsumer;
        }

        public void run()
        {
            getDataImmediate(dataConsumer);
        }
        
    }

}
