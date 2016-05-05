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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.sched.ExecutorService;
import org.raven.tree.impl.BaseNodeWithStat;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class HttpServerNode extends BaseNodeWithStat {
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
    
    private EventLoopGroup acceptorGroup;
    private EventLoopGroup workerGroup;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        acceptorGroup = null;
        workerGroup = null;
    }
    
    @Override
    protected void doStart() throws Exception {
        super.doStart();
        acceptorGroup = new NioEventLoopGroup(acceptorThreadsCount);
        workerGroup = new NioEventLoopGroup(workerThreadsCount);
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
