/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.impl;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionEventHandlerIF;
import org.productivity.java.syslog4j.util.SyslogUtility;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.net.SyslogMessageHandler;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.util.NodeUtils;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class SyslogReaderNode extends BaseNode implements SyslogServerSessionEventHandlerIF, Task
{
    public enum SyslogProtocol {UDP, TCP};

    @NotNull @Parameter(defaultValue="UDP")
    private SyslogProtocol protocol;
    
    @NotNull @Parameter(defaultValue="514")
    private Integer port;

    @Parameter(readOnly=true)
    private OperationStatistic messagesStat;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    private SyslogServerIF syslogServer;

    private AtomicBoolean taskRunning;

    @Override
    protected void initFields()
    {
        super.initFields();
        messagesStat = new OperationStatistic();
        taskRunning = new AtomicBoolean();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        if (taskRunning.get())
            throw new Exception("Can't start server because of syslog server task is still running");
        String _protocol = protocol.equals(SyslogProtocol.UDP)? "udp" : "tcp";
        syslogServer = SyslogServer.getInstance(_protocol);
        syslogServer.getConfig().setPort(port);
        syslogServer.getConfig().addEventHandler(this);
        syslogServer.getConfig().setShutdownWait(0);
//        syslogServer.getConfig().setHost("localhost");
//        syslogServer = SyslogServer.getInstance(_protocol);
        executor.execute(this);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        syslogServer.shutdown();
    }

    public Node getTaskNode() {
        return this;
    }

    public String getStatusMessage() {
        return String.format("Waiting for syslog message (port: %s, protocol: %s, received messages: %s)"
                , syslogServer.getConfig().getPort(), protocol, messagesStat.getOperationsCount());
    }

    public void run() {
        taskRunning.set(true);
        try {
            syslogServer.run();
        } finally {
            taskRunning.set(false);
        }
    }

    public OperationStatistic getMessagesStat() {
        return messagesStat;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public SyslogProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(SyslogProtocol protocol) {
        this.protocol = protocol;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Object sessionOpened(SyslogServerIF syslogServer, SocketAddress socketAddress) {
        if (isLogLevelEnabled(LogLevel.TRACE)) {
            getLogger().trace("Syslog session opened");
        }
        return this;
    }

    public void event(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress
            , SyslogServerEventIF event) 
    {
        long startTime = messagesStat.markOperationProcessingStart();
        logMessage(event);
        try {
            for (SyslogMessageHandler handler: NodeUtils.getChildsOfType(this, SyslogMessageHandler.class))
                handler.handleEvent(event);
        } finally {
            messagesStat.markOperationProcessingEnd(startTime);
        }
    }

    public void exception(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress
            , Exception exception) 
    {
        if (isLogLevelEnabled(LogLevel.ERROR))
            getLogger().error("Exeption throwed by Syslog server", exception);
    }

    public void sessionClosed(Object session, SyslogServerIF syslogServer
            , SocketAddress socketAddress, boolean timeout) 
    {
        if (isLogLevelEnabled(LogLevel.TRACE))
            getLogger().trace("Syslog session were closed");
    }

//    public void event(SyslogServerIF syslogServer, SyslogServerEventIF event)
//    {
//        long startTime = messagesStat.markOperationProcessingStart();
//        logMessage(event);
//        try {
//            for (SyslogMessageHandler handler: NodeUtils.getChildsOfType(this, SyslogMessageHandler.class))
//                handler.handleEvent(event);
//        } finally {
//            messagesStat.markOperationProcessingEnd(startTime);
//        }
//    }

    public void initialize(SyslogServerIF syslogServer) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Syslog server initialized on port {} using protocol {}"
                    , syslogServer.getConfig().getPort(), syslogServer.getProtocol());
    }

    public void destroy(SyslogServerIF syslogServer) {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Syslog server destroyed");
    }

    private void logMessage(SyslogServerEventIF event)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug(String.format(
                    "Received a message from (%s): Facility - %s, Level - %s, Message - %s"
                    , event.getHost(), SyslogUtility.getFacilityString(event.getFacility())
                    , SyslogUtility.getLevelString(event.getLevel()), event.getMessage()));
    }
}
