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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.raven.Helper;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractThreadedDataSource
        extends ContainerNode implements DataSource, RejectedExecutionHandler, NodeListener
{
    public static final String INTERVAL_ATTRIBUTE = "interval";
    public static final String INTERVAL_UNIT_ATTRIBUTE = "intervalUnit";
    
    @Parameter(defaultValue="3")
    private Integer corePoolSize;
    
    @Parameter(defaultValue="6")
    private Integer maximumPoolSize;
    
    private ScheduledThreadPoolExecutor executorService;
    private Collection<NodeAttribute> consumerAttributes;

    @Message
    private String intervalDescription;

    @Override
    protected void initFields() 
    {
        super.initFields();
        executorService = null;
        consumerAttributes = new ArrayList<NodeAttribute>();
    }

    public AbstractThreadedDataSource()
    {
        NodeAttribute attr = new NodeAttributeImpl(
                    INTERVAL_ATTRIBUTE, Integer.class, null, intervalDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(
                    INTERVAL_UNIT_ATTRIBUTE, TimeUnit.class, TimeUnit.MINUTES, 
                    "the time unit of the interval attribute");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        fillConsumerAttributes(consumerAttributes);
    }

    @Override
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        context.addSessionAttributes(
                dataConsumer instanceof Node? ((Node)dataConsumer).getNodeAttributes() : null
                , false);

        if (!checkDataConsumer(dataConsumer, context.getSessionAttributes()))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Skiping gathering data for data consumer (%s). Data consumer not ready"
                        , dataConsumer.getPath()));
            return false;
        }
        try
        {
            return gatherDataForConsumer(dataConsumer, context);
        }
        catch (Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Error gathering data for consumer (%s). %s"
                        , dataConsumer.getPath(), e.getMessage()));
            return false;
        }
    }

    public abstract boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext data)
            throws Exception;
    
    /**
     * Use this method to add attributes that consumers must have and set. The filled collection
     * will be returned by the {@link org.raven.tree.AttributesGenerator#generateAttributes()}
     * method.
     * @param consumerAttributes the collection 
     */
    public abstract void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes);

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        executorService = 
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(corePoolSize);
        executorService.setMaximumPoolSize(maximumPoolSize);
        executorService.setRejectedExecutionHandler(this);
        executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        
        if (getDependentNodes()!=null)
            for (Node node: getDependentNodes())
                if (node instanceof DataConsumer && node.getStatus()==Status.STARTED)
                    addDataConsumer((DataConsumer) node);
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
            removeDataConsumer((DataConsumer) dependentNode);
        
        return removed;
    }

    protected void addDataConsumer(DataConsumer dataConsumer)
    {
        int interval = dataConsumer instanceof Node? 
            (Integer)((Node)dataConsumer).getNodeAttribute(INTERVAL_ATTRIBUTE).getRealValue() : 0;
        if (interval<=0)
            return;
        TimeUnit unit = (TimeUnit) (dataConsumer instanceof Node ?
            ((Node) dataConsumer).getNodeAttribute(INTERVAL_UNIT_ATTRIBUTE).getRealValue()
            : TimeUnit.SECONDS);
        
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

    @Parameter(readOnly=true)
    public Integer getActiveThreads()
    {
        return Status.STARTED!=getStatus()? 0 : executorService.getActiveCount();
    }

    @Parameter(readOnly=true)
    public Integer getThreadsInPool()
    {
        return Status.STARTED!=getStatus()? 0 : executorService.getPoolSize();
    }

    @Parameter(readOnly=true)
    public Long getTaskCount()
    {
        return Status.STARTED!=getStatus()? 0 : executorService.getTaskCount();
    }

    @Parameter(readOnly=true)
    public Long getCompletedTaskCount()
    {
        return Status.STARTED!=getStatus()? 0 : executorService.getCompletedTaskCount();
    }

    @Parameter(readOnly=true)
    public Integer getLargestPoolSize()
    {
        return Status.STARTED!=getStatus()? 0: executorService.getLargestPoolSize();
    }

    public Integer getCorePoolSize()
    {
        return corePoolSize;
    }

    public Integer getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        if (this==node && executorService!=null)
        {
            if (attribute.getName().equals("corePoolSize"))
                executorService.setCorePoolSize(corePoolSize);
            else if (attribute.getName().equals("maximumPoolSize"))
                executorService.setMaximumPoolSize(maximumPoolSize);
        }
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
        if (getStatus()==Status.STARTED)
        {
            if (newStatus==Status.STARTED)
                addDataConsumer((DataConsumer) node);
            else
                removeDataConsumer((DataConsumer) node);
        }
    }
    
    protected boolean checkDataConsumer(
            DataConsumer consumer, Map<String, NodeAttribute> attributes)
    {
        return  !(consumer instanceof Node) || ((Node)consumer).getStatus()==Status.STARTED
                && Helper.checkAttributes(this, consumerAttributes, consumer, attributes);
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
            getDataImmediate(dataConsumer, new DataContextImpl());
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
