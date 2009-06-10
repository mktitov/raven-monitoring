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
import org.raven.DataCollector;
import org.raven.RavenCoreTestCase;
import org.raven.ds.Record;

/**
 *
 * @author Mikhail Titov
 */
public class SyslogReaderNodeTest extends RavenCoreTestCase
{
    private SyslogReaderNode reader;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        SyslogRecordSchemaNode schema = new SyslogRecordSchemaNode();
        schema.setName("syslog schema");
        tree.getRootNode().addAndSaveChildren(schema);
        assertTrue(schema.start());

        reader = new SyslogReaderNode();
        reader.setName("reader");
        tree.getRootNode().addAndSaveChildren(reader);
        reader.setProtocol(SyslogReaderNode.SyslogProtocol.UDP);
        reader.setPort(1514);
        reader.setSchema(schema);
        assertTrue(reader.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(reader);
        assertTrue(collector.start());
    }

    @Test
    public void test() throws Exception
    {
        SyslogIF syslog = Syslog.getInstance("udp");
        syslog.getConfig().setPort(1514);
        syslog.getConfig().setHost("localhost");
        syslog.getConfig().setFacility(SyslogConstants.FACILITY_FTP);
        Timestamp messDate = new Timestamp(System.currentTimeMillis());
        TimeUnit.SECONDS.sleep(1);
        syslog.info("hello world");

        TimeUnit.SECONDS.sleep(1);

        List dataList = collector.getDataList();
        assertEquals(1, dataList.size());

        Object data = dataList.get(0);
        assertNotNull(data);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        Timestamp ts = (Timestamp) rec.getValue(SyslogRecordSchemaNode.DATE_FIELD);
        assertTrue(ts.after(messDate));
        assertTrue(ts.before(new Timestamp(System.currentTimeMillis())));
        assertEquals("localhost", rec.getValue(SyslogRecordSchemaNode.HOST_FIELD));
        assertEquals(
                SyslogUtility.getFacilityString(SyslogConstants.FACILITY_FTP),
                rec.getValue(SyslogRecordSchemaNode.FACILITY_FIELD));
        assertEquals(
                SyslogUtility.getLevelString(SyslogConstants.LEVEL_INFO),
                rec.getValue(SyslogRecordSchemaNode.LEVEL_FIELD));
        assertTrue(((String)rec.getValue(SyslogRecordSchemaNode.MESSAGE_FIELD))
                .endsWith("hello world"));
    }
}