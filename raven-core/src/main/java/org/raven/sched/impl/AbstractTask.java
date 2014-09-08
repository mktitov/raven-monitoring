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

import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.log.LogLevel;
import org.raven.sched.CancelableTask;
import org.raven.sched.CancelationProcessor;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractTask implements CancelableTask
{
    private final Node taskNode;
    private final String status;
    private volatile CancelationProcessor cancelationProcessor;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private volatile boolean executed = false;
    
    public AbstractTask(Node taskNode, String status) {
        this.taskNode = taskNode;
        this.status = status;
    }

    public Node getTaskNode() {
        return taskNode;
    }

    public String getStatusMessage() {
        return status;
    }

    public void setCancelationProcessor(CancelationProcessor cancelationProcessor) {
        this.cancelationProcessor = cancelationProcessor;
    }

    public void cancel() {
        if (canceled.compareAndSet(false, true)) {
            final CancelationProcessor _processor = cancelationProcessor;
            if (_processor!=null)
                _processor.cancel();
        }
    }

    public boolean isExecuted() {
        return executed;
    }
    
    public boolean isCanceled() {
        return canceled.get();
    }

    public void run() {
        try {
            if (!canceled.get()) {
                try {
                    doRun();
                } finally {
                    executed = true;
                }
            }
        } catch(Exception e) {
            if (taskNode.isLogLevelEnabled(LogLevel.ERROR))
                taskNode.getLogger().error(String.format("Error executing (%s)", status), e);
        }
    }

    public abstract void doRun() throws Exception;
}
