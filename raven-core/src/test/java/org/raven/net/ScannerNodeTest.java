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

package org.raven.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.net.objects.TestScannerConsumer;
import org.raven.net.objects.TestScannerDataSource;
import org.raven.snmp.SnmpNode;
import org.raven.table.Table;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
public class ScannerNodeTest extends RavenCoreTestCase
{
    @Test 
    public void simpleTest() throws Exception
    {
        TestScannerDataSource ds = new TestScannerDataSource();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        ScannerNode scanner = new ScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setDataSource(ds);
        scanner.setThreadCount(5);
        scanner.setIpRanges("10.50.1.0-10.50.1.1, 10.50.2.0-10.50.2.0");
        scanner.setInterval(0);
        scanner.setIntervalUnit(TimeUnit.SECONDS);

        TestScannerConsumer consumer = new TestScannerConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.setDataSource(scanner);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        scanner.start();
        assertEquals(Status.STARTED, Status.STARTED);
        TimeUnit.SECONDS.sleep(1);

        List<String> ips = ds.getIps();
        assertEquals(3, ips.size());
        assertTrue(ips.contains("10.50.1.0"));
        assertTrue(ips.contains("10.50.1.1"));
        assertTrue(ips.contains("10.50.2.0"));
        
        Table table = consumer.getTable();
        assertNotNull(table);
        assertArrayEquals(new String[]{"ip"}, table.getColumnNames());
        List<String> rows = new ArrayList<String>();
        for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
            rows.add((String) it.next()[0]);
        assertEquals(2, rows.size());
        assertTrue(rows.contains("10.50.1.0"));
        assertTrue(rows.contains("10.50.2.0"));
    }
    
//    @Test 
    public void ipAddressFilterTest() throws Exception
    {
        TestScannerDataSource ds = new TestScannerDataSource();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        ScannerNode scanner = new ScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setDataSource(ds);
        scanner.setIpRanges("10.50.1.0-10.50.1.1, 10.50.2.0-10.50.2.0");
        scanner.setInterval(0);
        scanner.setIntervalUnit(TimeUnit.SECONDS);
        scanner.setIpAddressFilter(false);

        TestScannerConsumer consumer = new TestScannerConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.setDataSource(scanner);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());

        scanner.start();
        assertEquals(Status.STARTED, Status.STARTED);
        TimeUnit.SECONDS.sleep(1);

        Table table = consumer.getTable();
        assertNotNull(table);
        assertArrayEquals(new String[]{"ip"}, table.getColumnNames());
        List<String> rows = new ArrayList<String>();
        for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
            rows.add((String) it.next()[0]);
        assertEquals(0, rows.size());
    }

//    @Test
    public void snmpScanTest() throws Exception
    {
        SnmpNode snmp = new SnmpNode();
        snmp.setName("snmp");
        tree.getRootNode().addChildren(snmp);
        snmp.save();
        snmp.init();
        snmp.start();
        assertEquals(Status.STARTED, snmp.getStatus());

        ScannerNode scanner = new ScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setThreadCount(1);
        scanner.setIpRanges("10.50.1.16-10.50.1.16, 10.50.1.85-10.50.1.85");
        scanner.setInterval(0);
        scanner.setIntervalUnit(TimeUnit.SECONDS);
        scanner.setDataSource(snmp);
        scanner.getNodeAttribute(SnmpNode.TIMEOUT_ATTR).setValue("200");
        scanner.getNodeAttribute(SnmpNode.OID_ATTR).setValue("1.3.6.1.2.1.25.3.2.1.1.1");
        scanner.getNodeAttribute(SnmpNode.HOST_ATTR).setValue("dummyValue");
        
        TestScannerConsumer consumer = new TestScannerConsumer();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        consumer.setDataSource(scanner);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
        
        scanner.start();
        assertEquals(Status.STARTED, scanner.getStatus());
        TimeUnit.SECONDS.sleep(2);

        Table table = consumer.getTable();
        assertNotNull(table);
    }
}
