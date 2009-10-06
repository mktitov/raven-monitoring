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

package org.raven.net.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.test.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.ds.impl.AbstractThreadedDataSource;
import org.raven.snmp.objects.SnmpDataConsumer;
import org.raven.table.Table;
import org.raven.tree.Node.Status;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.smi.VariantVariable;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class SnmpReaderNodeTest extends ServiceTestCase
{
    private Tree tree;
    private TreeStore store;
    private TypeConverter converter;
    
    @Before
    public void initTest()
    {
        tree = registry.getService(Tree.class);
        store = registry.getService(Configurator.class).getTreeStore();
        store.removeNodes();
        converter = registry.getService(TypeConverter.class);
    }
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        super.configureRegistry(builder);
        builder.add(RavenCoreModule.class);
    }
    
//    @Ignore
    @Test 
    public void snmp4j() throws Exception
    {
        UdpAddress addr = new UdpAddress("kompr00205/161");
        
        CommunityTarget target = new CommunityTarget(addr, new OctetString("public"));
        target.setRetries(1);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version1);
        
        PDU pdu = new PDU();
//        pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.25.3.2.1.1.1")));
        VariableBinding bind = new  VariableBinding(
                new OID(".1.3.6.1.2.1.1.1.0"));
        pdu.add(bind);
        pdu.add(new VariableBinding(new OID(".1.3.6.1.2.1.6.13.1.1.0.0.0.0.7.0.0.0.0.10301")));
        pdu.setType(PDU.GET);
        
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();
        try
        {
            Snmp snmp = new Snmp(transport);
            ResponseEvent res = snmp.send(pdu, target);
            if (res.getError()!=null)
                throw res.getError();
            assertNotNull(res.getResponse());
            PDU resPdu = res.getResponse();
            List<VariableBinding> vars =  resPdu.getVariableBindings();
            assertTrue(vars.size()>0);
            for (VariableBinding var: vars)
            {
                System.out.println(""+var.getOid()+"="+var.getVariable().toString());
                System.out.println("Type: "+var.getVariable().getSyntaxString());
                System.out.println("Type: "+var.getVariable().getSyntax());
            }
            System.out.println("BIND ="+bind.getVariable().toString());
        }finally
        {
            transport.close();
        }
        fail();
    }
    
    @Test
    @Ignore
    public void tableReadTest() throws Exception
    {
        UdpAddress addr = new UdpAddress("127.0.0.1/161");
        
        CommunityTarget target = new CommunityTarget(addr, new OctetString("public"));
        target.setRetries(1);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version1);
        
        OID tableOID = new OID(".1.3.6.1.2.1.2.2");
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(tableOID));
        pdu.setType(PDU.GETNEXT);
        
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();
        try
        {
            Snmp snmp = new Snmp(transport);
            {
                ResponseEvent res = snmp.send(pdu, target);
                if (res.getError()!=null)
                    throw res.getError();
                assertNotNull(res.getResponse());
                pdu = res.getResponse();
                List<VariableBinding> vars =  pdu.getVariableBindings();
                assertTrue(vars.size()>0);
                VariableBinding var = vars.get(0);
                System.out.println(""+var.getOid()+"="+var.getVariable().toString());
                pdu.setType(PDU.GETNEXT);
            } while ( ((VariableBinding)pdu.getVariableBindings().get(0)).getOid().startsWith(tableOID) );
        }finally
        {
            transport.close();
        }
    }
    
    @Ignore
    @Test
    public void singleValueTest() throws Exception
    {
        SnmpReaderNode snmpNode = new SnmpReaderNode();
        snmpNode.setName("snmp");
        tree.getRootNode().addChildren(snmpNode);
        tree.saveNode(snmpNode);
        snmpNode.init();
        snmpNode.start();
        assertEquals(Status.STARTED, snmpNode.getStatus());
        
        SnmpDataConsumer consumer = new SnmpDataConsumer();
        consumer.setName("snmp-consumer");
        tree.getRootNode().addChildren(consumer);
        tree.saveNode(consumer);
        consumer.init();
        
        consumer.getNodeAttribute(
                AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(snmpNode.getPath());
        consumer.getNodeAttribute(AbstractThreadedDataSource.INTERVAL_ATTRIBUTE).setValue("2");
        consumer.getNodeAttribute(AbstractThreadedDataSource.INTERVAL_UNIT_ATTRIBUTE).setValue(
                TimeUnit.SECONDS.toString());
        consumer.getNodeAttribute(SnmpReaderNode.HOST_ATTR).setValue("localhost");
        consumer.getNodeAttribute(SnmpReaderNode.OID_ATTR).setValue("1.3.6.1.2.1.1.3.0");
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
        
        TimeUnit.SECONDS.sleep(3);
        
        snmpNode.stop();
        
        assertNotNull(consumer.getData());
        assertSame(snmpNode, consumer.getSource());
    }
    
    @Test
    @Ignore
    public void tableValueTest() throws Exception
    {
        store.removeNodes();
        tree.reloadTree();
        
        SnmpReaderNode snmpNode = new SnmpReaderNode();
        snmpNode.setName("snmp");
        tree.getRootNode().addChildren(snmpNode);
        tree.saveNode(snmpNode);
        snmpNode.init();
        snmpNode.start();
        assertEquals(Status.STARTED, snmpNode.getStatus());
        
        SnmpDataConsumer consumer = new SnmpDataConsumer();
        consumer.setName("snmp-consumer");
        tree.getRootNode().addChildren(consumer);
        tree.saveNode(consumer);
        consumer.init();
        
        consumer.getNodeAttribute(
                AbstractDataConsumer.DATASOURCE_ATTRIBUTE).setValue(snmpNode.getPath());
        consumer.getNodeAttribute(AbstractThreadedDataSource.INTERVAL_ATTRIBUTE).setValue("2");
        consumer.getNodeAttribute(AbstractThreadedDataSource.INTERVAL_UNIT_ATTRIBUTE).setValue(
                TimeUnit.SECONDS.toString());
        consumer.getNodeAttribute(SnmpReaderNode.HOST_ATTR).setValue("localhost");
        consumer.getNodeAttribute(SnmpReaderNode.OID_ATTR).setValue(".1.3.6.1.2.1.2.2");
        consumer.getNodeAttribute(
                SnmpReaderNode.OID_TYPE_ATTR).setValue(SnmpReaderNode.OidType.TABLE.toString());
        
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
        snmpNode.getDataImmediate(consumer, null);
        TimeUnit.SECONDS.sleep(3);
        
        snmpNode.stop();
        
        assertSame(snmpNode, consumer.getSource());
        assertNotNull(consumer.getData());
        assertTrue(consumer.getData() instanceof Table);
        
        Table table = (Table) consumer.getData();
        int ind = table.getColumnIndex("1.3.6.1.2.1.2.2.1.2");
        assertTrue(ind>0);
        Object val = table.getRowIterator().next()[ind];
        assertNotNull(val);
        assertEquals("lo", converter.convert(String.class, val, null));
    }
    
}
