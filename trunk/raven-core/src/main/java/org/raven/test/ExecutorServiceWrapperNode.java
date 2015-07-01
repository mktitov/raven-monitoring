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

package org.raven.test;

import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class ExecutorServiceWrapperNode extends BaseNode implements ExecutorService {

    private ExecutorService wrapper;

    public ExecutorService getWrapper() {
        return wrapper;
    }

    public void setWrapper(ExecutorService wrapper) {
        this.wrapper = wrapper;
    }

    public void execute(Task task) throws ExecutorServiceException {
        wrapper.execute(task);
    }

    public void execute(long delay, Task task) throws ExecutorServiceException {
        wrapper.execute(delay, task);
    }

    public boolean executeQuietly(Task task) {
        return wrapper.executeQuietly(task);
    }

    public boolean executeQuietly(long delay, Task task) {
        return wrapper.executeQuietly(delay, task);
    }
}
