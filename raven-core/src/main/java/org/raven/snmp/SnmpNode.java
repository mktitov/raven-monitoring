/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.snmp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.raven.table.Table;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
@Description("The data source node that gathers data using snmp.")
public class SnmpNode extends AbstractDataSource
{
    public enum OidType {SINGLE, TABLE};
    
    public final static String ROW_INDEX_COLUMN_NAME = "index";
    
    public static final String PORT_ATTR = "snmp-port";
    public static final String VERSION_ATTR = "snmp-version";
    public static final String COMMUNITY_ATTR = "snmp-community";
    public static final String HOST_ATTR = "host";
    public static final String OID_ATTR = "OID";
    public static final String OID_TYPE_ATTR = "OID-Type";
    
    @Override
    public void gatherDataForConsumer(DataConsumer dataConsumer) throws Exception 
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format(
                    "Gathering data for data consumer (%s)", dataConsumer.getPath()));
        String host = dataConsumer.getNodeAttribute(HOST_ATTR).getRealValue();
        Integer port = dataConsumer.getNodeAttribute(PORT_ATTR).getRealValue();
        SnmpVersion version = dataConsumer.getNodeAttribute(VERSION_ATTR).getRealValue();
        String community = dataConsumer.getNodeAttribute(COMMUNITY_ATTR).getRealValue();
        String oid = dataConsumer.getNodeAttribute(OID_ATTR).getRealValue();
        boolean isTable =
                dataConsumer.getNodeAttribute(OID_TYPE_ATTR).getRealValue()==OidType.TABLE;

        UdpAddress address = new UdpAddress(host+"/"+port);
        CommunityTarget target = new CommunityTarget(address, new OctetString(community));
        target.setRetries(1);
        target.setTimeout(2000);
        target.setVersion(version.asInt());

        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oid)));

        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp  = new Snmp(transport);
        try
        {
            snmp.listen();

            Object value = isTable?
                getTableValue(snmp, target, pdu) : getSimpleValue(snmp, target, pdu);

            dataConsumer.setData(this, value);
        }
        finally
        {
            snmp.close();
        }
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttributeImpl attr = 
                new NodeAttributeImpl(
                    HOST_ATTR, String.class, null
                    , "The ip address or the domain name of the device");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(PORT_ATTR, Integer.class, 161, "The snmp port");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(
                COMMUNITY_ATTR, String.class, "public", "The snmp community name");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(OID_ATTR, String.class, null, "The Object Identifier Class");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(
                VERSION_ATTR, SnmpVersion.class, SnmpVersion.V1, "The version of the SNMP");
        attr.setRequired(true);
        consumerAttributes.add(attr);
        
        attr = new NodeAttributeImpl(
               OID_TYPE_ATTR, OidType.class, OidType.SINGLE, "The type of the OID");
        attr.setRequired(true);
        consumerAttributes.add(attr);
    }
    
    private Object getSimpleValue(Snmp snmp, Target target, PDU pdu) throws Exception
    {
        pdu.setType(PDU.GET);
        ResponseEvent response = snmp.send(pdu, target);
        if (response.getError()!=null)
            throw response.getError();
        pdu = response.getResponse();
        if (pdu==null)
            throw new NodeError("Response timeout");
        
        return pdu.get(0).getVariable();
    }

    private Object getTableValue(Snmp snmp, CommunityTarget target, PDU pdu) throws Exception
    {
        OID tableOID = pdu.get(0).getOid();
        Table table = new TableImpl();
        Set<Integer> rowIndexes = new HashSet<Integer>();
        while (true) 
        {
            pdu.setType(PDU.GETNEXT);
            ResponseEvent response = snmp.send(pdu, target);
            if (response.getError()!=null)
                throw response.getError();
            pdu = response.getResponse();
            if (pdu==null)
                throw new NodeError("Response timeout");
            VariableBinding var = pdu.get(0);
            
            if (var.getOid().startsWith(tableOID))
            {
                OID columnOid = new OID(var.getOid());
                int index = columnOid.last();
                if (!rowIndexes.contains(index))
                {
                    table.addValue(ROW_INDEX_COLUMN_NAME, index);
                    rowIndexes.add(index);
                }
                columnOid.removeLast();
                String columnName = columnOid.toString();
                table.addValue(columnName, var.getVariable());
            }
            else
                break;
        }
        
        return table;
    }
}
