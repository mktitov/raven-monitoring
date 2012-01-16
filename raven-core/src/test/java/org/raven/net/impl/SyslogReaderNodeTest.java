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

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.SyslogIF;
import org.productivity.java.syslog4j.util.SyslogUtility;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.Record;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;

/**
 *
 * @author Mikhail Titov
 */
public class SyslogReaderNodeTest extends RavenCoreTestCase
{
    private SyslogReaderNode reader;
    private DataCollector collector;
    private DataCollector collector2;
    private ExecutorServiceNode executor;

    @Before
    public void prepare() throws Exception
    {
        SyslogRecordSchemaNode schema = new SyslogRecordSchemaNode();
        schema.setName("syslog schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        executor = new ExecutorServiceNode();
        executor.setName("executor");
        tree.getRootNode().addAndSaveChildren(executor);
        assertTrue(executor.start());

        reader = new SyslogReaderNode();
        reader.setName("reader");
        tree.getRootNode().addAndSaveChildren(reader);
        reader.setProtocol(SyslogReaderNode.SyslogProtocol.UDP);
        reader.setPort(1515);
        reader.setExecutor(executor);
        reader.setLogLevel(LogLevel.TRACE);
        assertTrue(reader.start());

        SyslogMessageHandlerNode handler = new SyslogMessageHandlerNode();
        handler.setName("handler1");
        reader.addAndSaveChildren(handler);
//        handler.getNodeAttribute(SyslogMessageHandlerNode.ACCEPT_MESSAGE_EXPRESSION_ATTR)
//                .setValue(
//                    "facility=='ftp' && host=='localhost' " +
//                    "&& message.contains('hello world') && level=='INFO'");
        handler.getNodeAttribute(SyslogMessageHandlerNode.ACCEPT_MESSAGE_EXPRESSION_ATTR)
                .setValue(
                    "facility=='ftp' " +
                    "&& message.contains('hello world') && level=='INFO'");
        handler.setRecordSchema(schema);
        handler.setLogLevel(LogLevel.TRACE);
        assertTrue(handler.start());

        SyslogMessageHandlerNode handler2 = new SyslogMessageHandlerNode();
        handler2.setName("handler2");
        reader.addAndSaveChildren(handler2);
        handler2.getNodeAttribute(SyslogMessageHandlerNode.ACCEPT_MESSAGE_EXPRESSION_ATTR)
                .setValue("facility=='cron'");
        handler2.setLogLevel(LogLevel.TRACE);
        handler2.setRecordSchema(schema);
        assertTrue(handler2.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(handler);
        assertTrue(collector.start());

        collector2 = new DataCollector();
        collector2.setName("collector2");
        tree.getRootNode().addAndSaveChildren(collector2);
        collector2.setDataSource(handler2);
        assertTrue(collector2.start());
    }

    @Test
    public void test() throws Exception
    {
        SyslogIF syslog = Syslog.getInstance(Syslog.UDP);
        syslog.getConfig().setPort(1515);
        syslog.getConfig().setHost("localhost");
//        syslog.getConfig().setHost("10.50.1.85");
//        syslog.getConfig().setHost("10.50.12.4");
        syslog.getConfig().setFacility(SyslogConstants.FACILITY_FTP);
        Timestamp messDate = new Timestamp(System.currentTimeMillis());
        TimeUnit.SECONDS.sleep(1);
        reader.getLogger().debug("!!!Sending hello world message");
        syslog.info("hello world");
        syslog.flush();

        TimeUnit.SECONDS.sleep(2);

        List dataList = collector.getDataList();
        assertEquals(1, dataList.size());

        Object data = dataList.get(0);
        assertNotNull(data);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        Timestamp ts = (Timestamp) rec.getValue(SyslogRecordSchemaNode.DATE_FIELD);
        assertTrue(ts.after(messDate));
        assertTrue(ts.before(new Timestamp(System.currentTimeMillis())));
//        assertEquals("localhost", rec.getValue(SyslogRecordSchemaNode.HOST_FIELD));
        assertEquals(
                SyslogUtility.getFacilityString(SyslogConstants.FACILITY_FTP),
                rec.getValue(SyslogRecordSchemaNode.FACILITY_FIELD));
        assertEquals(
                SyslogUtility.getLevelString(SyslogConstants.LEVEL_INFO),
                rec.getValue(SyslogRecordSchemaNode.LEVEL_FIELD));
        assertTrue(((String)rec.getValue(SyslogRecordSchemaNode.MESSAGE_FIELD))
                .endsWith("hello world"));

        assertEquals(0, collector2.getDataList().size());

        collector.getDataList().clear();

        syslog.getConfig().setFacility(SyslogConstants.FACILITY_CRON);
        syslog.info("hello world");
        TimeUnit.SECONDS.sleep(2);
        
        assertEquals(1, collector2.getDataList().size());
        assertEquals(0, collector.getDataList().size());
    }
}