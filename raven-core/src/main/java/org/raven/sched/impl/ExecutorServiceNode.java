/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.sched.impl;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SchedulersNode.class)
public class ExecutorServiceNode extends BaseNode
        implements ExecutorService, RejectedExecutionHandler
{
	@NotNull @Parameter(defaultValue="2")
	private Integer corePoolSize;

	@NotNull @Parameter(defaultValue="6")
	private Integer maximumPoolSize;

	@NotNull @Parameter(defaultValue="60")
	private Long keepAliveTime;

	@NotNull @Parameter(defaultValue="SECONDS")
	private TimeUnit timeUnit;

    @Parameter
    private Integer maximumQueueSize;

    @Parameter(readOnly=true)
    private OperationStatistic executedTasks;

    @Parameter(readOnly=true)
    private AtomicLong rejectedTasks;

	private ThreadPoolExecutor executor;
    private BlockingQueue queue;
    private Collection executingTasks;
    private AtomicLong taskIdCounter;

    @Override
    protected void initFields()
    {
        super.initFields();
        executor = null;
        queue = null;
        executingTasks = null;
        rejectedTasks = new AtomicLong();
        executedTasks = new OperationStatistic();
        taskIdCounter = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        resetStatFields();
        Integer capacity = maximumQueueSize;
        if (capacity==null)
            queue = new LinkedBlockingQueue();
        else
            queue = new LinkedBlockingQueue(capacity);
        executingTasks = new ConcurrentSkipListSet();
		executor = new ThreadPoolExecutor(
                corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, queue, this);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        executor.shutdown();
        resetStatFields();
        executingTasks = null;
    }

    public void execute(Task task) throws ExecutorServiceException
    {
        if (Status.STARTED.equals(getStatus()))
            executor.execute(new TaskWrapper(task));
    }

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor)
    {
        rejectedTasks.incrementAndGet();
        if (isLogLevelEnabled(LogLevel.ERROR))
            error(String.format(
                    "Error executing task for node (%s). Executor service does not have " +
                    "the free thread for task execution"
                    , ((Task)r).getTaskNode().getPath()));
    }

    @Parameter(readOnly=true)
    public Integer getExecutingTaskCount() {
        return executingTasks==null? null : executingTasks.size();
    }

    public OperationStatistic getExecutedTasks() {
        return executedTasks;
    }

    public AtomicLong getRejectedTasks() {
        return rejectedTasks;
    }

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public Long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Integer getMaximumQueueSize() {
        return maximumQueueSize;
    }

    public void setMaximumQueueSize(Integer maximumQueueSize) {
        this.maximumQueueSize = maximumQueueSize;
    }

    private void resetStatFields()
    {
        executedTasks.reset();
        rejectedTasks.set(0l);
        taskIdCounter.set(0l);
    }

    private class TaskWrapper implements Task, Comparable
    {
        private final Task task;
        private final Long id;

        public TaskWrapper(Task task)
        {
            this.task = task;
            id = taskIdCounter.incrementAndGet();
        }

        public Long getId() {
            return id;
        }

        public Node getTaskNode() 
        {
            return task.getTaskNode();
        }

        public String getStatusMessage()
        {
            return task.getStatusMessage();
        }

        public void run()
        {
            long executionStart = executedTasks.markOperationProcessingStart();
            try
            {
                try
                {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format(
                                "Executing task for node (%s)", task.getTaskNode().getPath()));
                    executingTasks.add(this);
                    task.run();
                }
                catch(Throwable e)
                {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error(
                                String.format(
                                    "Error executing task for node (%s)"
                                    , task.getTaskNode().getPath())
                                , e);
                }
            }
            finally
            {
                executedTasks.markOperationProcessingEnd(executionStart);
                executingTasks.remove(this);
            }
        }

        public int compareTo(Object o)
        {
            return id.compareTo(((TaskWrapper)o).getId());
        }
    }
}
