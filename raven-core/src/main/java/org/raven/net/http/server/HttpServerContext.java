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
import org.raven.cache.TemporaryFileManager;
import org.raven.dp.DataProcessorFacade;
import org.raven.net.NetworkResponseService;
import org.raven.net.http.server.impl.ErrorPageGeneratorImpl;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public interface HttpServerContext {
    /**
     * Returns the active connections counter
     */
    public AtomicLong getActiveConnectionsCounter();
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
    /**
     * Returns the error page generator
     */
    public ErrorPageGenerator getErrorPageGenerator();
    /**
     * Returns the type of the http protocol
     */
    public Protocol getProtocol();
    /**
     * Returns the facade of the DP that periodically calls {@link ChannelTimeoutChecker#checkTimeoutIfNeed(long) } 
     */
    public DataProcessorFacade getConnectionManager();
    /**
     * Returns the socket read timeout in ms.
     */
    public long getReadTimeout();
    /**
     * Returns the keep-alive timeout
     */
    public long getKeepAliveTimeout();
    /**
     * Returns the default response build timeout. The build timeout can be also defined by response builder node
     */
    public long getDefaultResponseBuildTimeout();
    
    /**
     * Returns the temporary file where uploaded file will be stored
     */
    public String getUploadedFilesTempDir();
    /**
     * Returns the temporary file manager that will be used by http server
     */
    public TemporaryFileManager getTempFileManager();
}
