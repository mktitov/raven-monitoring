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
package org.raven.net.http.server.impl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.audit.Auditor;
import org.raven.net.NetworkResponseService;
import org.raven.net.http.server.HttpServerContext;
import org.raven.sched.ExecutorService;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNodeWithStat;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class HttpServerNode extends BaseNodeWithStat {
    @Service
    private static NetworkResponseService networkResponseService;
    @Service
    private static Auditor auditor;
    
    @NotNull @Parameter()
    private Integer port;
    
    @NotNull @Parameter(defaultValue = "1")
    private Integer acceptorThreadsCount;
    
    @NotNull @Parameter(defaultValue = "4")
    private Integer workerThreadsCount;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useSSL;
    
    @Parameter()
    private String keystorePath;
    
    @Parameter()
    private String keystorePassword;
    
    @NotNull @Parameter
    private ExecutorService executor;
    
    @NotNull @Parameter(defaultValue = "1024")
    private Integer responseStreamBufferSize;
    
    @NotNull @Parameter(defaultValue = "4096")
    private Integer responseStreamMaxPendingBytesForWrite;   
    
    @NotNull @Parameter(defaultValue = "32")
    private Integer requestStreamBuffersCount;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean alwaysExecuteBuilderInExecutor;
    
    @Parameter(readOnly = true)
    private AtomicLong  connectionsCount;
    
    @Parameter(readOnly = true)
    private AtomicLong requestsCount;
    
    @Parameter(readOnly = true)
    private AtomicLong writtenBytes;
    
    @Parameter(readOnly = true)
    private AtomicLong readBytes;
    
    private EventLoopGroup acceptorGroup;
    private EventLoopGroup workerGroup;
    private HttpServerContext serverContext;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        acceptorGroup = null;
        workerGroup = null;
        connectionsCount = new AtomicLong();
        requestsCount = new AtomicLong();
        writtenBytes = new AtomicLong();
        readBytes = new AtomicLong();
        serverContext = null;
    }
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        acceptorGroup = new NioEventLoopGroup(acceptorThreadsCount);
        workerGroup = new NioEventLoopGroup(workerThreadsCount);
        serverContext = new ServerContextImpl(connectionsCount, requestsCount, writtenBytes, readBytes, 
                networkResponseService, this, executor, auditor, 
                responseStreamBufferSize, responseStreamMaxPendingBytesForWrite, requestStreamBuffersCount,
                alwaysExecuteBuilderInExecutor, converter);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(acceptorGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(null);
        } catch (Exception e) {
            acceptorGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Integer getResponseStreamBufferSize() {
        return responseStreamBufferSize;
    }

    public void setResponseStreamBufferSize(Integer responseStreamBufferSize) {
        this.responseStreamBufferSize = responseStreamBufferSize;
    }

    public Integer getResponseStreamMaxPendingBytesForWrite() {
        return responseStreamMaxPendingBytesForWrite;
    }

    public void setResponseStreamMaxPendingBytesForWrite(Integer responseStreamMaxPendingBytesForWrite) {
        this.responseStreamMaxPendingBytesForWrite = responseStreamMaxPendingBytesForWrite;
    }

    public Integer getRequestStreamBuffersCount() {
        return requestStreamBuffersCount;
    }

    public void setRequestStreamBuffersCount(Integer requestStreamBuffersCount) {
        this.requestStreamBuffersCount = requestStreamBuffersCount;
    }

    public Boolean getAlwaysExecuteBuilderInExecutor() {
        return alwaysExecuteBuilderInExecutor;
    }

    public void setAlwaysExecuteBuilderInExecutor(Boolean alwaysExecuteBuilderInExecutor) {
        this.alwaysExecuteBuilderInExecutor = alwaysExecuteBuilderInExecutor;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getAcceptorThreadsCount() {
        return acceptorThreadsCount;
    }

    public void setAcceptorThreadsCount(Integer acceptorThreadsCount) {
        this.acceptorThreadsCount = acceptorThreadsCount;
    }

    public Integer getWorkerThreadsCount() {
        return workerThreadsCount;
    }

    public void setWorkerThreadsCount(Integer workerThreadsCount) {
        this.workerThreadsCount = workerThreadsCount;
    }

    public Boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(Boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }        

    public AtomicLong getConnectionsCount() {
        return connectionsCount;
    }

    public AtomicLong getRequestsCount() {
        return requestsCount;
    }

    public AtomicLong getWrittenBytes() {
        return writtenBytes;
    }

    public AtomicLong getReadBytes() {
        return readBytes;
    }
    
    private static class ServerContextImpl implements HttpServerContext {
        private final AtomicLong connectionsCounter;
        private final AtomicLong requestsCounter;
        private final AtomicLong writtenBytesCounter;
        private final AtomicLong readBytesCounter;
        private final NetworkResponseService responseService;
        private final Node owner;
        private final ExecutorService executor;
        private final Auditor auditor;
        private final int responseStreamBufferSize;
        private final int responseStreamMaxPendingBytesForWrite;
        private final int requestStreamBuffersCount;
        private final boolean alwaysExecuteBuilderInExecutor;
        private final TypeConverter typeConverter;

        public ServerContextImpl(AtomicLong connectionsCounter, AtomicLong requestsCounter, 
                AtomicLong writtenBytesCounter, AtomicLong readBytesCounter, 
                NetworkResponseService responseService, Node owner, ExecutorService executor, 
                Auditor auditor, int responseStreamBufferSize, int responseStreamMaxPendingBytesForWrite,
                int requestStreamBuffersCount, boolean alwaysExecuteBuilderInExecutor,
                TypeConverter typeConverter) 
        {
            this.connectionsCounter = connectionsCounter;
            this.requestsCounter = requestsCounter;
            this.writtenBytesCounter = writtenBytesCounter;
            this.readBytesCounter = readBytesCounter;
            this.responseService = responseService;
            this.owner = owner;
            this.executor = executor;
            this.auditor = auditor;
            this.responseStreamBufferSize = responseStreamBufferSize;
            this.responseStreamMaxPendingBytesForWrite = responseStreamMaxPendingBytesForWrite;
            this.requestStreamBuffersCount = requestStreamBuffersCount;
            this.alwaysExecuteBuilderInExecutor = alwaysExecuteBuilderInExecutor;
            this.typeConverter = typeConverter;
        }

        @Override
        public Auditor getAuditor() {
            return auditor;
        }

        @Override
        public AtomicLong getConnectionsCounter() {
            return connectionsCounter;
        }

        @Override
        public AtomicLong getRequestsCounter() {
            return requestsCounter;
        }

        @Override
        public AtomicLong getWrittenBytesCounter() {
            return writtenBytesCounter;
        }

        @Override
        public AtomicLong getReadBytesCounter() {
            return readBytesCounter;
        }

        @Override
        public NetworkResponseService getNetworkResponseService() {
            return responseService;
        }

        @Override
        public ExecutorService getExecutor() {
            return executor;
        }

        @Override
        public Node getOwner() {
            return owner;
        }

        @Override
        public int getResponseStreamBufferSize() {
            return responseStreamBufferSize;
        }

        @Override
        public int getResponseStreamMaxPendingBytesForWrite() {
            return responseStreamMaxPendingBytesForWrite;
        }

        @Override
        public int getRequestStreamBuffersCount() {
            return requestStreamBuffersCount;
        }

        @Override
        public boolean getAlwaysExecuteBuilderInExecutor() {
            return alwaysExecuteBuilderInExecutor;
        }

        @Override
        public TypeConverter getTypeConverter() {
            return typeConverter;
        }
    }
    
    private static class Initializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
//            ch.pipeline()
//                    .addLast(new HttpRequestDecoder())
//                    .addLast(new HttpResponseEncoder())
//                    .add
        }
        
    }
}
