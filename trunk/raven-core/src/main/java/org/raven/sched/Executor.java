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
package org.raven.sched;

/**
 *
 * @author Mikhail Titov
 */
public interface Executor {
    /**
     * Executes the task in separate thread
     */
    public void execute(Task task) throws ExecutorServiceException;
    /**
     * Executes the task in separate thread through <code>delay</code> milliseconds.
     */
    public void execute(long delay, Task task) throws ExecutorServiceException;
    /**
     * The same as {@link #execute(org.raven.sched.Task)} but this method does not throws exception
     * @return <b>true</b> if tasks successfully
     */
    public boolean executeQuietly(Task task);
    /**
     * The same as {@link #execute(long, org.raven.sched.Task)} but this method does not throws exception
     */
    public boolean executeQuietly(long delay, Task task);
    
}
