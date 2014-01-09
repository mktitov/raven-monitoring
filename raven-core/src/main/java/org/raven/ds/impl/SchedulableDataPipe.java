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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionViewableObject;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SchedulableDataPipe extends SafeDataPipeNode
        implements Schedulable, Scheduler, Task, Viewable
{
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull
    private Scheduler scheduler;

    @NotNull @Parameter(defaultValue="false")
    private Boolean allowAsyncExecution;

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @Message
    private String displayMessage;
    @Message
    private String executedMessage;

    private ReentrantLock lock;

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantLock();
    }

    public void executeScheduledJob(Scheduler scheduler) {
        if (!isStarted()) return;
        try{
            if (allowAsyncExecution)
                doExecuteJob();
            else {
                if (lock.tryLock()) {
                    try{
                        doExecuteJob();
                    } finally {
                        lock.unlock();
                    }
                } else if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Can't execute schedulable task. Already executing");
            }
        }catch(Exception e){
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error executing scheduled job", e);
        }
    }

    public void run()
    {
        executeScheduledJob(null);
    }

    private void doExecuteJob() throws Exception
    {
        if (isLogLevelEnabled(LogLevel.DEBUG)) {
            debug("Initiating data gathering request");
        }
//        getDataSource().getDataImmediate(this, new DataContextImpl());
        gatherDataForConsumer(null, new DataContextImpl());
        Collection<Node> deps = getDependentNodes();
        if (deps != null) 
            for (Node node : deps) 
                if (node instanceof Schedulable) 
                    ((Schedulable) node).executeScheduledJob(this);
    }

    public String getStatusMessage()
    {
        return "Scheduled job immediate executing";
    }

    public Node getTaskNode()
    {
        return this;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception 
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception
    {
        ExecutorService _executor = executor;
        if (!Status.STARTED.equals(getStatus()) || _executor==null)
            return null;

        return Arrays.asList(
                (ViewableObject)
                new ExecuteAction(displayMessage, executedMessage, this, _executor));
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Boolean getAllowAsyncExecution() {
        return allowAsyncExecution;
    }

    public void setAllowAsyncExecution(Boolean allowAsyncExecution) {
        this.allowAsyncExecution = allowAsyncExecution;
    }

    private class ExecuteAction extends AbstractActionViewableObject
    {
        private final ExecutorService executor;
        private final String executedMessage;

        public ExecuteAction(String displayMessage
                , String executedMessage, Node owner, ExecutorService executor)
        {
            super(null, displayMessage, owner, false);
            this.executor = executor;
            this.executedMessage = executedMessage;
        }

        @Override
        public String executeAction() throws Exception
        {
            executor.execute(SchedulableDataPipe.this);
            return executedMessage;
        }
    }
}
