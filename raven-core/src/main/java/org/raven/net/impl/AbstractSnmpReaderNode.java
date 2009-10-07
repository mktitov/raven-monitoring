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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractThreadedDataSource;
import org.raven.log.LogLevel;
import org.raven.net.SnmpVersion;
import org.raven.net.impl.SnmpReaderNode.OidType;
import org.raven.table.ColumnBasedTable;
import org.raven.table.Table;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.NodeAttributeImpl;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSnmpReaderNode extends AbstractThreadedDataSource
{
    public enum OidType {SINGLE, TABLE};

    public final static String ROW_INDEX_COLUMN_NAME = "index";

    public static final String PORT_ATTR = "snmp-port";
    public static final String TIMEOUT_ATTR = "snmp-timeout";
    public static final String VERSION_ATTR = "snmp-version";
    public static final String COMMUNITY_ATTR = "snmp-community";
    public static final String HOST_ATTR = "host";
    public static final String OID_TYPE_ATTR = "OID-Type";

    @Message
    private static String hostDescription;
    @Message
    private static String snmpPort;
    @Message
    private static String timeoutDescription;
    @Message
    private static String snmpCommunityDescription;
    @Message
    private static String oidDescription;
    @Message
    private static String snmpVersionDescription;
    @Message
    private static String oidTypeDescription;

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format(
                    "Gathering data for data consumer (%s)", dataConsumer.getPath()));
        String host = attributes.get(HOST_ATTR).getRealValue();
        Integer port = attributes.get(PORT_ATTR).getRealValue();
        SnmpVersion version = attributes.get(VERSION_ATTR).getRealValue();
        String community = attributes.get(COMMUNITY_ATTR).getRealValue();
        boolean isTable = attributes.get(OID_TYPE_ATTR).getRealValue()==OidType.TABLE;
        Long timeout = attributes.get(TIMEOUT_ATTR).getRealValue();

        UdpAddress address = new UdpAddress(host+"/"+port);
        CommunityTarget target = new CommunityTarget(address, new OctetString(community));
        target.setRetries(1);
        target.setTimeout(timeout);
        target.setVersion(version.asInt());

        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp  = new Snmp(transport);
        try
        {
            snmp.listen();
            try
            {
                proccessSnmpRequest(dataConsumer, snmp, target, attributes, isTable);
            }
            catch(Exception e)
            {
                if (dataConsumer instanceof Node)
                {
                    Node consumer = (Node) dataConsumer;
                    if (consumer.isLogLevelEnabled(LogLevel.ERROR))
                        consumer.getLogger().error("Error processing snmp request", e);
                }
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format(
                            "Error processing snmp request for consumer (%s)"
                            , dataConsumer.getPath()
                        , e));
            }
        }
        finally
        {
            snmp.close();
        }
        return true;
    }

    protected abstract void proccessSnmpRequest(
            DataConsumer dataConsumer, Snmp snmp, CommunityTarget target
            , Map<String, NodeAttribute> attrs, boolean isTable)
        throws Exception;

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttributeImpl attr =
                new NodeAttributeImpl(HOST_ATTR, String.class, null, hostDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(PORT_ATTR, Integer.class, 161, snmpPort);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                TIMEOUT_ATTR, Long.class, 2000, timeoutDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                COMMUNITY_ATTR, String.class, "public", snmpCommunityDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                VERSION_ATTR, SnmpVersion.class, SnmpVersion.V1, snmpVersionDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
               OID_TYPE_ATTR, OidType.class, OidType.SINGLE, oidTypeDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);
    }

    protected Table getTableValue(Snmp snmp, CommunityTarget target, PDU pdu) throws Exception
    {
        OID tableOID = pdu.get(0).getOid();
        int colOidSize = tableOID.size()+2;
        ColumnBasedTable table = new ColumnBasedTable();
        Set<String> rowIndexes = new HashSet<String>();
        while (true)
        {
            pdu.setType(PDU.GETNEXT);
            ResponseEvent response = snmp.send(pdu, target);
            if (response.getError()!=null)
                throw response.getError();
            pdu = response.getResponse();
            if (pdu==null)
                throw new NodeError("Response timeout");
            if (pdu.getErrorIndex()!=0)
                throw new NodeError(pdu.getErrorStatusText());
            VariableBinding var = pdu.get(0);

            if (var.getOid().startsWith(tableOID))
            {
                OID columnOid = new OID(var.getOid().getValue(), 0, colOidSize);
                OID indexOid = new OID(
                        var.getOid().getValue(), colOidSize, var.getOid().size()-colOidSize);
                String index = indexOid.toString();
                if (!rowIndexes.contains(index))
                {
                    table.addValue(ROW_INDEX_COLUMN_NAME, index);
                    rowIndexes.add(index);
                }
                table.addValue(columnOid.toString(), var.getVariable());
            }
            else
                break;
        }
        table.freeze();
        return table;
    }

}
