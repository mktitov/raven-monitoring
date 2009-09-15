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

package org.raven.rep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.List;
import net.sf.jasperreports.engine.JasperExportManager;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.DummyScheduler;
import org.raven.test.RavenCoreTestCase;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.ConnectionPool;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.sched.Scheduler;
import org.raven.tree.NodeError;
import org.raven.tree.impl.ParameterNode;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class SqlJasperReportTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        ReportRecordSchemaNode reportSchema = createReportRecordSchema();
        ConnectionPool connectionPool = createConnectionPool();
        Scheduler scheduler = createScheduler();

        SqlJasperReport report = new SqlJasperReport();
        report.setName("report");
        tree.getRootNode().addAndSaveChildren(report);
        report.setReportType("report type");
        report.setReportName("report name");
        report.setConnectionPool(connectionPool);
        report.setRecordSchema(reportSchema);
        report.setScheduler(scheduler);

        File reportFile = new File("src/test/conf/Report1.jasper");
        FileInputStream is = new FileInputStream(reportFile);
        report.getReportFile().setDataStream(is);
        report.getReportFile().setFilename("report1.jasper");
        is.close();
        ParameterNode parameter = new ParameterNode();
        parameter.setName("TITLE");
        report.getReportParameters().addAndSaveChildren(parameter);
        parameter.setParameterValue("Nodes report");
        assertTrue(parameter.start());

        assertTrue(report.start());

        DataCollector dataCollector = createDataCollector(report);

        long startTime = System.currentTimeMillis();
        report.executeScheduledJob(null);
        long endTime = System.currentTimeMillis();
        List<Object> dataList = dataCollector.getDataList();
        assertNotNull(dataList);
        assertEquals(2, dataList.size());

        assertNull(dataList.get(1));
        Object data = dataList.get(0);
        assertNotNull(data);
        assertTrue(data instanceof Record);
        Record rec = (Record) data;
        assertEquals("report type", rec.getValue(ReportRecordSchemaNode.TYPE_FIELD_NAME));
        assertEquals("report name", rec.getValue(ReportRecordSchemaNode.NAME_FIELD_NAME));
        Timestamp generationDate =
                (Timestamp) rec.getValue(ReportRecordSchemaNode.GENERATIONDATE_FIELD_NAME);
        assertNotNull(generationDate);
        long ts = generationDate.getTime();
        assertTrue(ts>=startTime && ts<=endTime);

        BinaryFieldType repData = (BinaryFieldType) rec.getValue(
                ReportRecordSchemaNode.REPORTDATA_FIELD_NAME);
        FileOutputStream fos = new FileOutputStream("target/report1.pdf");
        JasperExportManager.exportReportToPdfStream(repData.getData(), fos);
        fos.close();
    }

    private ConnectionPool createConnectionPool() throws Exception
    {
        Config conf = configurator.getConfig();
        assertNotNull(conf);

        ConnectionPoolsNode poolsNode =
                (ConnectionPoolsNode)
                tree.getNode(SystemNode.NAME).getChildren(ConnectionPoolsNode.NAME);
        assertNotNull(poolsNode);
        JDBCConnectionPoolNode pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        poolsNode.addChildren(pool);
        pool.save();
        pool.init();

        pool.setUserName(conf.getStringProperty(Configurator.TREE_STORE_USER, null));
        pool.setPassword(conf.getStringProperty(Configurator.TREE_STORE_PASSWORD, null));
        pool.setUrl(conf.getStringProperty(Configurator.TREE_STORE_URL, null));
        pool.setDriver("org.h2.Driver");
        assertTrue(pool.start());

        return pool;
    }

    private DataCollector createDataCollector(DataSource dataSource)
    {
        DataCollector dataCollector = new DataCollector();
        dataCollector.setName("dataCollector");
        tree.getRootNode().addAndSaveChildren(dataCollector);
        dataCollector.setDataSource(dataSource);
        assertTrue(dataCollector.start());

        return dataCollector;
    }

    private ReportRecordSchemaNode createReportRecordSchema() throws NodeError
    {
        ReportRecordSchemaNode reportSchema = new ReportRecordSchemaNode();
        reportSchema.setName("report schema");
        tree.getRootNode().addAndSaveChildren(reportSchema);
        assertTrue(reportSchema.start());

        return reportSchema;
    }

    private Scheduler createScheduler()
    {
        DummyScheduler scheduler = new DummyScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        return scheduler;
    }
}