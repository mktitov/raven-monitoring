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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.test.DummyScheduler;
import org.raven.test.RavenCoreTestCase;
import org.raven.net.objects.TestScannerConsumer;
import org.raven.net.objects.TestScannerDataSource;
import org.raven.table.Table;
import org.raven.tree.Node.Status;

/**
 *
 * @author Mikhail Titov
 */
@Ignore
public class NetworkScannerNodeTest extends RavenCoreTestCase
{
    private DummyScheduler scheduler;
    
    @Before
    public void beforeTest()
    {
        scheduler = new DummyScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addChildren(scheduler);
        scheduler.save();
        scheduler.init();
        scheduler.start();
    }

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

        NetworkScannerNode scanner = new NetworkScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setScheduler(scheduler);
        scanner.setDataSource(ds);
        scanner.setThreadCount(5);
        scanner.setIpRanges("10.50.1.0-10.50.1.1, 10.50.2.0-10.50.2.0");
        scanner.setInterval(0);
        scanner.setIntervalUnit(TimeUnit.SECONDS);
        scanner.setResolveHostnames(false);

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
        scanner.executeScheduledJob(null);
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
    
    @Test 
    public void ipAddressFilterTest() throws Exception
    {
        TestScannerDataSource ds = new TestScannerDataSource();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        ds.start();
        assertEquals(Status.STARTED, ds.getStatus());

        NetworkScannerNode scanner = new NetworkScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setScheduler(scheduler);
        scanner.setDataSource(ds);
        scanner.setIpRanges("10.50.1.0-10.50.1.1, 10.50.2.0-10.50.2.0");
        scanner.setInterval(0);
        scanner.setIntervalUnit(TimeUnit.SECONDS);
        scanner.setIpAddressFilter(false);
        scanner.setResolveHostnames(false);

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
        scanner.executeScheduledJob(null);
        TimeUnit.SECONDS.sleep(1);

        Table table = consumer.getTable();
        assertNotNull(table);
        assertArrayEquals(new String[]{"ip"}, table.getColumnNames());
        List<String> rows = new ArrayList<String>();
        for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
            rows.add((String) it.next()[0]);
        assertEquals(0, rows.size());
    }

    @Test
    public void snmpScanTest() throws Exception
    {
        SnmpNode snmp = new SnmpNode();
        snmp.setName("snmp");
        tree.getRootNode().addChildren(snmp);
        snmp.save();
        snmp.init();
        snmp.start();
        assertEquals(Status.STARTED, snmp.getStatus());

        NetworkScannerNode scanner = new NetworkScannerNode();
        scanner.setName("scanner");
        tree.getRootNode().addChildren(scanner);
        scanner.save();
        scanner.init();
        scanner.setScheduler(scheduler);
        scanner.setThreadCount(10);
//        scanner.setIpRanges("10.50.0.0-10.50.255.255");
        scanner.setIpRanges("10.50.1.0-10.50.1.255");
        scanner.setInterval(0);
        scanner.setIntervalUnit(TimeUnit.SECONDS);
        scanner.setDataSource(snmp);
        scanner.getNodeAttribute(SnmpNode.TIMEOUT_ATTR).setValue("100");
//        scanner.getNodeAttribute(SnmpNode.OID_ATTR).setValue("1.3.6.1.2.1.25.3.2.1.1.1");
        scanner.getNodeAttribute(SnmpNode.OID_ATTR).setValue("1.3.6.1.2.1.25.3.5.1.1.1");
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
        scanner.executeScheduledJob(null);
        
        while (scanner.isScanning())
            TimeUnit.MILLISECONDS.sleep(100);

        Table table = consumer.getTable();
        assertNotNull(table);

        System.out.println("start time: "+scanner.getScanningStartTime());
        System.out.println("duration: "+scanner.getScanningDuration());
        System.out.println("scanned ips: "+scanner.getIpsScanned());
        System.out.println("ips found: "+scanner.getIpsFound());
        System.out.println("speed ips/sec: "+scanner.getScanningSpeed());
        for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
        {
            Object[] row = it.next();
            System.out.println("Printer ip: "+row[0]+"; hostname: "+row[1]);
        }
    }
}
