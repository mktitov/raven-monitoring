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
package org.raven.sched.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.SystemExecutorService;
import org.raven.sched.Task;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = SchedulersNode.class, childNodes = {ExecutorServiceNode.class})
public class ExecutorServiceBalancerNode extends BaseNode implements ExecutorService {
    
    @Service
    private static SystemExecutorService systemExecutor;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useSystemExecutorAsFailover;
    
    private final static ExecutorService[] EMPTY_ARR = new ExecutorService[]{};
    private AtomicReference<ExecutorService[]> executors;
    private AtomicInteger counter;
    private AtomicLong errorsCount;
    private AtomicLong executedTasksCount;
    private AtomicLong executedDelayedTasksCount;
    private AtomicLong executedBySystemTasksCount;
    private AtomicLong executedDelayedBySystemTasksCount;
    private volatile long lastLogTime;
    private AtomicBoolean loggingNow;

    @Override
    protected void initFields() {
        super.initFields();
        executors = new AtomicReference<>();
        counter = new AtomicInteger();
        errorsCount = new AtomicLong();
        lastLogTime = 0l;
        loggingNow = new AtomicBoolean();
        executedTasksCount = new AtomicLong();
        executedDelayedTasksCount = new AtomicLong();
        executedBySystemTasksCount = new AtomicLong();
        executedDelayedBySystemTasksCount = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        rebuildExecutors();
    }

    @Override
    public void addChildren(Node node) {
        super.addChildren(node); 
        rebuildExecutors();
    }

    @Override
    public void removeChildren(Node node) {
        super.removeChildren(node);
        rebuildExecutors();
    }

    @Override
    public void detachChildren(Node node) {
        super.detachChildren(node); 
        rebuildExecutors();
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus) {
        rebuildExecutors();
    }
    
    private void rebuildExecutors() {
        List<ExecutorService> _executors = NodeUtils.getChildsOfType(this, ExecutorService.class);
        if (_executors.isEmpty())
            executors.set(null);
        else 
            executors.set(_executors.toArray(EMPTY_ARR));
    }
    
    private ExecutorService getExecutor() {
        final ExecutorService[] _executors = executors.get();
        return _executors==null? null : _executors[Math.abs(counter.getAndIncrement())%_executors.length];
    }
    
    private int getMaxTries() {
        final ExecutorService[] _executors = executors.get();
        return _executors==null? 0 : _executors.length;
    }
    
    private void logError() {
        final long curTime = System.currentTimeMillis();
        if (isErrorEnabled() && lastLogTime+1000<curTime && loggingNow.compareAndSet(false, true)) {
            lastLogTime = curTime;
            getLogger().error("Problem with executing task. Total errors: "+errorsCount.incrementAndGet());
            loggingNow.set(false);
        }
    }
    
    @Override
    public void execute(Task task) throws ExecutorServiceException {
        if (isStarted()) {
            for (int i=0; i<getMaxTries(); ++i) {
                ExecutorService executor = getExecutor();
                if (executor==null)
                    break;
                try {
                    executor.execute(task);
                    executedTasksCount.incrementAndGet();
                    return;
                } catch (ExecutorServiceException e) {
                }
            }
            //
            if (useSystemExecutorAsFailover) {
                systemExecutor.getExecutor().execute(task);
                executedBySystemTasksCount.incrementAndGet();
                return;
            }
        }
        logError();
        final String message = "Problem with executing a task: "+task.getTaskNode();
        throw new ExecutorServiceException(message);
    }
    
//    private void 

    @Override
    public void execute(long delay, Task task) throws ExecutorServiceException {
        if (isStarted()) {
            for (int i=0; i<getMaxTries(); ++i) {
                ExecutorService executor = getExecutor();
                if (executor==null)
                    break;
                try {
                    executor.execute(delay, task);
                    executedDelayedTasksCount.incrementAndGet();
                    return;
                } catch (ExecutorServiceException e) {
                }
            }
            //
            if (useSystemExecutorAsFailover) {
                systemExecutor.getExecutor().execute(delay, task);
                executedDelayedBySystemTasksCount.incrementAndGet();
                return;
            }
        }
        logError();
        final String message = "Problem with executing a task: "+task.getTaskNode();
        throw new ExecutorServiceException(message);
    }

    @Override
    public boolean executeQuietly(Task task) {
        try {
            execute(task);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean executeQuietly(long delay, Task task) {
        try {
            execute(delay, task);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public Boolean getUseSystemExecutorAsFailover() {
        return useSystemExecutorAsFailover;
    }

    public void setUseSystemExecutorAsFailover(Boolean useSystemExecutorAsFailover) {
        this.useSystemExecutorAsFailover = useSystemExecutorAsFailover;
    }

    @Parameter(readOnly = true)
    public AtomicLong getExecutedTasksCount() {
        return executedTasksCount;
    }

    @Parameter(readOnly = true)
    public AtomicLong getExecutedDelayedTasksCount() {
        return executedDelayedTasksCount;
    }

    @Parameter(readOnly = true)
    public AtomicLong getExecutedBySystemTasksCount() {
        return executedBySystemTasksCount;
    }

    @Parameter(readOnly = true)
    public AtomicLong getExecutedDelayedBySystemTasksCount() {
        return executedDelayedBySystemTasksCount;
    }
    
}
