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
import java.util.Map;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.util.SyslogUtility;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.Record;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataSourcesNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class SyslogReaderNode extends AbstractDataSource implements SyslogServerEventHandlerIF
{
    public enum SyslogProtocol {UDP, TCP};

    @NotNull @Parameter(defaultValue="UDP")
    private SyslogProtocol protocol;
    
    @NotNull @Parameter(defaultValue="514")
    private Integer port;

    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode schema;

    private SyslogServerIF syslogServer;

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

    public RecordSchemaNode getSchema() {
        return schema;
    }

    public void setSchema(RecordSchemaNode schema) {
        this.schema = schema;
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        return true;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    public void event(SyslogServerIF syslogServer, SyslogServerEventIF event)
    {
        try
        {
            Record rec = schema.createRecord();
            rec.setValue(SyslogRecordSchemaNode.DATE_FIELD, event.getDate());
            rec.setValue(SyslogRecordSchemaNode.HOST_FIELD, event.getHost());
            rec.setValue(
                    SyslogRecordSchemaNode.FACILITY_FIELD
                    , SyslogUtility.getFacilityString(event.getFacility()));
            rec.setValue(
                    SyslogRecordSchemaNode.LEVEL_FIELD
                    , SyslogUtility.getLevelString(event.getLevel()));
            rec.setValue(SyslogRecordSchemaNode.MESSAGE_FIELD, event.getMessage());

            sendDataToConsumers(rec);
        }
        catch(Exception e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Error handling syslog event", e);
        }
    }
}
