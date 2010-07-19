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

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SchedulableDataPipe extends SafeDataPipeNode implements Schedulable, Scheduler
{
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull
    private Scheduler scheduler;

    @NotNull @Parameter(defaultValue="false")
    private Boolean allowAsyncExecution;

    private ReentrantLock lock;

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantLock();
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
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
    }

    private void doExecuteJob()
    {
        if (isLogLevelEnabled(LogLevel.DEBUG)) {
            debug("Initiating data gathering request");
        }
        getDataSource().getDataImmediate(this, new DataContextImpl());
        Collection<Node> deps = getDependentNodes();
        if (deps != null) {
            for (Node node : deps) {
                if (node instanceof Schedulable) {
                    ((Schedulable) node).executeScheduledJob(this);
                }
            }
        }
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
}
