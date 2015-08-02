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
package org.raven.dp;

import java.util.concurrent.TimeUnit;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.ds.TimeoutMessageSelector;
import org.raven.sched.ExecutorServiceException;

/**
 *
 * @author Mikhail Titov
 */
public interface DataProcessorFacade {
    public final static String TIMEOUT_MESSAGE = "TIMEOUT_MESSAGE";
    public final static String STOP_MESSAGE = "STOP_MESSAGE";
    public final static String TERMINATED_MESSAGE = "TERMINATED_MESSAGE"; 
    
    /**
     * Returns the name of the facade. Can return null
     */
    public String getName();
    
    public String getPath();
    
    /**
     * Returns true if processor was terminated
     */
    public boolean isTerminated();
    
    /**
     * Sends stop message to the data processor with {@link DataProcessorFacadeConfig#getDefaultStopTimeout()} timeout.
     * If processor will not stop after this timeout then data processor will be terminated.
     */
    public void stop();
    /**
     * Sends stop message to the data processor with {@link DataProcessorFacadeConfig#getDefaultStopTimeout()} timeout.
     * If processor will not stop after this timeout then data processor will be terminated.
     */
    public RavenFuture askStop();
    /**
     * Sends stop message to the data processor with timeout passed in the parameter.
     * If processor will not stop after this timeout then data processor will be terminated.
     */
    public RavenFuture askStop(long timeout, TimeUnit timeoutTimeUnit);
    /**
     * Returns the future that watches for data processor termination
     */
    public RavenFuture watch();
    /**
     * Adds data processor passed in the parameter for this data processor termination
     */
    public void watch(DataProcessorFacade watcher);
    /**
     * Remove processor passed in the parameter from watchers
     */
    public void unwatch(DataProcessorFacade watcher);
    
    /**
     * Sends message to data processor asynchronously 
     * @param message message to process
     * @return true if message successfully queued for process
     */
    public boolean send(Object message);
    /**
     * Sends message from this data processor facade to facade passed in the parameter <b>facade</b>
     * @param facade Receiver of the message. Receiver in this case received the reference to this facade
     * @param message message to process
     * @return true if message successfully queued for process
     */
    public boolean sendTo(DataProcessorFacade facade, Object message);
    /**
     * Sends message after delay
     * @param delay delay in milliseconds
     * @param message message to send
     */
    public void sendDelayed(long delay, Object message) throws ExecutorServiceException;
    /**
     * Sends message after delay to the data processor facade passed in the parameter
     * @param facade Receiver of the message. Receiver in this case received the reference to this facade
     * @param delay delay in milliseconds
     * @param message message to send
     */
    public void sendDelayedTo(DataProcessorFacade facade, long delay, Object message) throws ExecutorServiceException;
    /**
     * Sends message repeatedly 
     * @param delay initial delay in milliseconds
     * @param interval repeat interval in milliseconds
     * @param times number of times (zero unlimited)
     * @param message the message to send
     */
    public void sendRepeatedly(long delay, long interval, int times, Object message) throws ExecutorServiceException;
    /**
     * Sends message repeatedly to the data processor facade passed in the parameter
     * @param facade Receiver of the message. Receiver in this case received the reference to this facade
     * @param delay initial delay in milliseconds
     * @param interval repeat interval in milliseconds
     * @param times number of times (zero unlimited)
     * @param message the message to send
     */
    public void sendRepeatedlyTo(DataProcessorFacade facade, long delay, long interval, int times, Object message) 
            throws ExecutorServiceException;
    
    /**
     * Sends {@link TimeoutMessage timeout message} after being idle <b>timeout</b> milliseconds (no message send to data processor in passed interval)
     * @param timeout interval in milliseconds 
     */
    public void setReceiveTimeout(long timeout) throws ExecutorServiceException;
    /**
     * Sends {@link TimeoutMessage timeout message} after being idle <b>timeout</b> milliseconds (no message send to data processor in passed interval)
     * @param timeout interval in milliseconds 
     * @param selector Allows to select messages that reset timeout
     */
    public void setReceiveTimeout(long timeout, TimeoutMessageSelector selector) throws ExecutorServiceException;
    public void resetReceiveTimeout();
    
    /**
     * Sends message to data processor and return the future that's wait the response for this message with timeout passed
     * in the parameter timeout
     * @param message message that will be sent to the data processor
     * @param timeout timeout for waiting message
     * @param timeoutTimeUnit time unit for <b>timeout</b>
     */
    public RavenFuture ask(final Object message, final long timeout, final TimeUnit timeoutTimeUnit);
    /**
     * Sends message to data processor and return the future that's wait the response for this message with default ask timeout
     */
    public RavenFuture ask(Object message);
}
