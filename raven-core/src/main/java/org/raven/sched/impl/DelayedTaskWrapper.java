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

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.raven.sched.CancelableTask;
import org.raven.sched.CancelationProcessor;
import org.raven.sched.Task;

/**
 *
 * @author Mikhail Titov
 */
public class DelayedTaskWrapper  implements Delayed, CancelationProcessor {
    private final long startAt;
    private final Task task;
    private final DelayQueue<DelayedTaskWrapper> queue;

    public DelayedTaskWrapper(Task task, long delay, DelayQueue<DelayedTaskWrapper> queue) {
        this.task = task;
        this.startAt = System.currentTimeMillis()+delay;
        this.queue = queue;
        if (task instanceof CancelableTask)
            ((CancelableTask)task).setCancelationProcessor(this);
    }

    public long getStartAt() {
        return startAt;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(startAt-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o==this)
            return 0;
        long d = getDelay(TimeUnit.MILLISECONDS)-o.getDelay(TimeUnit.MILLISECONDS);
        return d==0? 0 : (d<0? -1 : 1);
    }

    @Override
    public void cancel() {
        queue.remove(this);
    }
}
