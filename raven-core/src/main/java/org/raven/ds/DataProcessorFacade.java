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
package org.raven.ds;

import org.raven.sched.ExecutorServiceException;

/**
 *
 * @author Mikhail Titov
 */
public interface DataProcessorFacade extends DataProcessor {
    public boolean isTerminated();
    public void terminate();
    /**
     * Same as {@link #processData(java.lang.Object)}
     * @param message message to process
     * @return true if message successfully queued for process
     */
    public boolean send(Object message);
    /**
     * Sends message after delay
     * @param delay delay in milliseconds
     * @param message message to send
     */
    public void sendDelayed(long delay, Object message) throws ExecutorServiceException;
    /**
     * Sends message repeatedly 
     * @param delay initial delay in milliseconds
     * @param interval repeat interval in milliseconds
     * @param times number of times (zero unlimited)
     * @param message the message to send
     */
    public void sendRepeatedly(long delay, long interval, int times, Object message) throws ExecutorServiceException;
    /**
     * sends {@link TimeoutMessage timeout message} after being idle <b>timeout</b> milliseconds (no message send to data processor in passed interval)
     * @param timeout interval in milliseconds 
     */
    public void setTimeout(long timeout, long checkTimeoutInterval) throws ExecutorServiceException;
}
