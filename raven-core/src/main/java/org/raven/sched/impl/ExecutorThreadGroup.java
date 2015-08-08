/*
 * Copyright 2015 Mikhail Titov
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.raven.sched.ExecutorTask;
import org.raven.sched.ExecutorTaskHolder;
import org.raven.sched.ExecutorThreadFactory;

/**
 *
 * @author Mikhail Titov
 */
public class ExecutorThreadGroup extends ThreadGroup implements ExecutorThreadFactory {

    public ExecutorThreadGroup(String groupName) {
        super(groupName);
    }
    
    @Override
    public int getRunningThreadsCount() {
        return activeCount();
    }

    @Override
    public List<ExecutorTask> getExecutingTasks() {        
        int size = activeCount();
        if (size==0)
            return Collections.EMPTY_LIST;
        else {
            ArrayList<ExecutorTask> tasks = new ArrayList<>(size);
            Thread[] threads = new Thread[size];
            int total = enumerate(threads);
            for (int i=0; i<total; ++i) {
                ExecutorTask task = getTask(threads[i]);
                if (task!=null)
                    tasks.add(task);
            }
            return tasks.isEmpty()? Collections.EMPTY_LIST : tasks;
        }
    }

    @Override
    public int getExecutingTasksCount() {
        int count = 0;
        int size = activeCount();
        if (size!=0) {
            Thread[] threads = new Thread[size];
            int total = enumerate(threads);
            for (int i=0; i<total; ++i) {
                ExecutorTask task = getTask(threads[i]);
                if (task!=null)
                    count++;
            }
        }
        return count;
    }
    
    private ExecutorTask getTask(Thread thread) {
        return thread instanceof ExecutorTaskHolder? ((ExecutorTaskHolder)thread).getExecutorTask() : null;
    }
    
}
