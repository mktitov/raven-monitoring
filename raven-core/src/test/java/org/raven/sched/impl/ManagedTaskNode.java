/*
 *  Copyright 2011 Mikhail Titov.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.annotations.Parameter;
import org.raven.sched.ExecutorService;
import org.raven.sched.ManagedTask;
import org.raven.sched.TaskRestartPolicy;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ManagedTaskNode extends BaseNode implements ManagedTask
{
    @NotNull @Parameter
    private ExecutorService executor;

    private TaskRestartPolicy taskRestartPolicy;
    private int executionCount;
    private int restartCount;
    private long sleepInterval;

    public int getExecutionCount() {
        return executionCount;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public long getSleepInterval() {
        return sleepInterval;
    }

    public void setSleepInterval(long sleepInterval) {
        this.sleepInterval = sleepInterval;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        restartCount++;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public TaskRestartPolicy getTaskRestartPolicy() {
        return taskRestartPolicy;
    }

    public void setTaskRestartPolicy(TaskRestartPolicy taskRestartPolicy) {
        this.taskRestartPolicy = taskRestartPolicy;
    }

    public Node getTaskNode() {
        return this;
    }

    public String getStatusMessage() {
        return "test message";
    }

    public void run() {
        executionCount++;
        try {
            Thread.sleep(sleepInterval);
        } catch (InterruptedException ex) {
        }
    }
}
