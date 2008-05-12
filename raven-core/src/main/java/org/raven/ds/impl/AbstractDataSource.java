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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataSource
        extends ContainerNode implements DataSource, RejectedExecutionHandler, NodeListener
{
    public static final String INTERVAL_ATTRIBUTE = "interval";
    public static final String INTERVAL_UNIT_ATTRIBUTE = "intervalUnit";
    @Parameter @Description("Sets the core number of threads")
    private int corePoolSize = 3;
    @Parameter @Description("Sets the maximum allowed number of threads")
    private int maximumPoolSize = 6;
    
    private ScheduledThreadPoolExecutor executorService = null;
    private Collection<NodeAttribute> consumerAttributes = new ArrayList<NodeAttribute>();

    public AbstractDataSource()
    {
        consumerAttributes.add(
                new NodeAttributeImpl(
                    INTERVAL_ATTRIBUTE, Integer.class, null, "the period between executions"));
        consumerAttributes.add(
                new NodeAttributeImpl(
                    INTERVAL_UNIT_ATTRIBUTE, TimeUnit.class, TimeUnit.MINUTES, 
                    "the time unit of the interval attribute"));
    }

    @Override
    public synchronized boolean start() throws NodeError
    {
        if (!super.start())
            return false;
                
        executorService = 
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(corePoolSize);
        executorService.setMaximumPoolSize(maximumPoolSize);
        executorService.setRejectedExecutionHandler(this);
        executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        
        if (getDependentNodes()!=null)
            for (Node node: getDependentNodes())
                if (node instanceof DataConsumer && node.getStatus()==Status.STARTED)
                    addDataConsumer((DataConsumer) node);
        
        return true;
    }

    @Override
    public synchronized void stop() throws NodeError
    {
        super.stop();
        
        executorService.shutdown();
        executorService = null;
    }

    @Override
    public synchronized boolean addDependentNode(Node dependentNode)
    {
        boolean nodeAdded = super.addDependentNode(dependentNode);
        if (nodeAdded)
        {
            if (dependentNode instanceof DataConsumer)
            {
                dependentNode.addListener(this);
                if ( getStatus()==Status.STARTED && dependentNode.getStatus()==Status.STARTED)
                {
                    addDataConsumer((DataConsumer)dependentNode);
                }
            }
        }
        
        return nodeAdded;
    }

    @Override
    public synchronized boolean removeDependentNode(Node dependentNode)
    {
        boolean removed =  super.removeDependentNode(dependentNode);
        
        if (removed && getStatus()==Status.STARTED && dependentNode instanceof DataConsumer)
        {
            removeDataConsumer((DataConsumer) dependentNode);
        }
        
        return removed;
    }

    protected void addDataConsumer(DataConsumer dataConsumer)
    {
        int interval = (Integer)dataConsumer.getNodeAttribute(INTERVAL_ATTRIBUTE).getRealValue();
        TimeUnit unit = dataConsumer.getNodeAttribute(INTERVAL_UNIT_ATTRIBUTE).getRealValue();
        
        executorService.scheduleAtFixedRate(new Task(dataConsumer), 0, interval, unit);
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return consumerAttributes;
    }

    protected void removeDataConsumer(DataConsumer dataConsumer)
    {
        executorService.remove(new Task(dataConsumer));
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

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void statusChanged(Node node, Status oldStatus, Status newStatus)
    {
        if (getStatus()==Status.STARTED)
        {
            if (newStatus==Status.STARTED)
                addDataConsumer((DataConsumer) node);
            else
                removeDataConsumer((DataConsumer) node);
        }
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

        @Override
        public boolean equals(Object obj)
        {
            if (this==obj)
                return true;
            if (obj instanceof Task)
                return dataConsumer.equals(((Task)obj).dataConsumer);
            else 
                return false;
        }

        @Override
        public int hashCode()
        {
            return dataConsumer.hashCode();
        }
    }

}
