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

import java.util.Collection;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.net.SyslogMessageHandler;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.util.OperationStatistic;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class SyslogReaderNode extends BaseNode implements SyslogServerEventHandlerIF
{
    public enum SyslogProtocol {UDP, TCP};

    @NotNull @Parameter(defaultValue="UDP")
    private SyslogProtocol protocol;
    
    @NotNull @Parameter(defaultValue="514")
    private Integer port;

    @Parameter(readOnly=true)
    private OperationStatistic messagesStat;

    private SyslogServerIF syslogServer;

    @Override
    protected void initFields()
    {
        super.initFields();
        messagesStat = new OperationStatistic();
    }

    @Override
    protected void doStart() throws Exception
    {
         super.doStart();
        String _protocol = protocol.equals(SyslogProtocol.UDP)? "udp" : "tcp";
        syslogServer = SyslogServer.getInstance(_protocol);
        syslogServer.getConfig().setPort(port);
        syslogServer.getConfig().addEventHandler(this);
        syslogServer = SyslogServer.getThreadedInstance(_protocol);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        syslogServer.shutdown();
    }

    public OperationStatistic getMessagesStat()
    {
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

    public void event(SyslogServerIF syslogServer, SyslogServerEventIF event)
    {
        long startTime = messagesStat.markOperationProcessingStart();
        try
        {
            Collection<Node> childs = getChildrens();
            if (childs!=null && !childs.isEmpty())
                for (Node child: childs)
                    if (child instanceof SyslogMessageHandler)
                        ((SyslogMessageHandler)child).handleEvent(event);
        }
        finally
        {
            messagesStat.markOperationProcessingEnd(startTime);
        }
    }
}
