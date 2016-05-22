/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server;

import java.util.concurrent.atomic.AtomicLong;
import org.raven.audit.Auditor;
import org.raven.net.NetworkResponseService;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public interface HttpServerContext {
    /**
     * Returns the connections counter
     */
    public AtomicLong getConnectionsCounter();
    /**
     * Returns the requests counter
     */
    public AtomicLong getRequestsCounter();
    /**
     * Returns counter for written bytes 
     */
    public AtomicLong getWrittenBytesCounter();
    /**
     * Returns the counter for read bytes
     */
    public AtomicLong getReadBytesCounter();
    /**
     * Returns the reference to the network response service
     */
    public NetworkResponseService getNetworkResponseService();
    /**
     * Returns the executor attached to the http server node
     */
    public ExecutorService getExecutor();
    /**
     * Returns the http server node
     */
    public Node getOwner();
    /**
     * Returns the auditor service
     */
    public Auditor getAuditor();
    /**
     * Returns the response buffer size
     */
    public int getResponseStreamBufferSize();
    /**
     * Returns the max bytes pending for write
     */
    public int getResponseStreamMaxPendingBytesForWrite();
    /**
     * Returns the maximum buffers count in request input stream
     */
    public int getRequestStreamBuffersCount();
    /**
     * If <b>true</b> then response builder will be executed in the executor thread otherwise, if possible, builder will
     * executed in the netty thread
     */
    public boolean getAlwaysExecuteBuilderInExecutor();
    /**
     * Returns the type converter service
     */
    public TypeConverter getTypeConverter();
}
