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
import org.raven.net.NetworkResponseService;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;

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
}
