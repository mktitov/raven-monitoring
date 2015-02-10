/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.rep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reportMatcher;
import static org.easymock.EasyMock.verify;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.TestScheduler;
import org.raven.cache.TemporaryFileManager;
import org.raven.cache.TemporaryFileManagerNode;
import org.raven.ds.DataContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class JettReportNodeTest extends RavenCoreTestCase {
    private TestScheduler scheduler;
    private PushDataSource ds;
    private JettReportNode report;
    private DataCollector collector;
    private TemporaryFileManagerNode tempFileManager;

    @Before
    public void prepare()
    {
        scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        tempFileManager = new TemporaryFileManagerNode();
        tempFileManager.setName("manager");
        testsNode.addAndSaveChildren(tempFileManager);
        tempFileManager.setDirectory("target/");
        tempFileManager.setScheduler(scheduler);
        assertTrue(tempFileManager.start());        
        
        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        report = new JettReportNode();
        report.setName("report");
        tree.getRootNode().addAndSaveChildren(report);
        report.setDataSource(ds);
        report.setLogLevel(LogLevel.TRACE);
        report.setTemporaryFileManager(tempFileManager);

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(report);
        assertTrue(collector.start());
    }

//    @Test
    public void test() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template.xlsx"));
        assertTrue(report.start());

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource(), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("hello world");

        verify(handler);
    }

//    @Test
    public void sheetListenerTest() throws Exception
    {
        SheetListenerNode listener = new SheetListenerNode();
        listener.setName("sheet listener");
        report.addAndSaveChildren(listener);
        listener.getAttr("sheetProcessed").setValue("event.sheet.getRow(0).getCell(0).setCellValue('hello world')");
        assertTrue(listener.start());
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jett_sheet_listener_template.xls"));        
        assertTrue(report.start());

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource(), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("hello world");

        verify(handler);
    }
    
//    @Test
    public void sheetListenerTest2() throws Exception {
        SheetListenerNode listener = new SheetListenerNode();
        listener.setName("sheet listener");
        report.addAndSaveChildren(listener);
        listener.getAttr("beforeSheetProcessed").setValue("false");
        assertTrue(listener.start());
        
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jett_sheet_listener_template2.xls"));
        assertTrue(report.start());

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource("<jt:style/>"), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("hello world");

        verify(handler);
    }


//    @Test
    public void beansGenerationTest() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template2.xls"));
        assertTrue(report.start());

        JxlsAttributeValueBeanNode attrValue = new JxlsAttributeValueBeanNode();
        attrValue.setName("bean1");
        report.addAndSaveChildren(attrValue);
        attrValue.getAttr("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attrValue.setValue("data+' world'");
        assertTrue(attrValue.start());

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource(), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("hello");

        verify(handler);
    }

//    @Test
    public void fixedSizeCollectionTest() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template4.xls"));
        assertTrue(report.start());

        JxlsAttributeValueBeanNode attrValue = new JxlsAttributeValueBeanNode();
        attrValue.setName("list");
        report.addAndSaveChildren(attrValue);
        attrValue.getAttr("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attrValue.setValue("[[col1:1, col2:1],[col1:5, col2:10]]");
        attrValue.setFixedSizeCollection(Boolean.TRUE);
        assertTrue(attrValue.start());

//        DataHandler handler = createMock(DataHandler.class);
//        handler.handleData(checkDataSource(), isA(DataContext.class));
//        replay(handler);

        collector.setDataHandler(new WriteToFileHandler("target/report_fixedSizeCollection.xls"));
        ds.pushData("test");
        ds.pushData(null);

//        verify(handler);
    }

    @Test
    public void multiSheetReportTest() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template2.xls"));
        report.setMultiSheetReport(Boolean.TRUE);
        NodeAttribute attr = report.getAttr(JettReportNode.SHEET_NAME_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("data");
        assertTrue(report.start());

        JxlsAttributeValueBeanNode attrValue = new JxlsAttributeValueBeanNode();
        attrValue.setName("bean1");
        report.addAndSaveChildren(attrValue);
        attrValue.getAttr("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attrValue.setValue("data");
        assertTrue(attrValue.start());

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource2(), isA(DataContext.class));
        handler.handleData(isNull(), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("data1");
        ds.pushData("data2");
        ds.pushData(null);

        verify(handler);
    }

//    @Test
    public void styleTest() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jett_style_template.xls"));
        report.setStyles(".red-font {font-color:red;}");
        assertTrue(report.start());

        collector.setDataHandler(new WriteToFileHandler("target/jett_style_test_report.xls"));
        ds.pushData("test");
        ds.pushData(null);
    }
    
    public static InputStream checkDataSource() {
        return checkDataSource("hello world");
    }
    
    public static InputStream checkDataSource(final String cellValue)
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object obj)
            {
                try{
                    if (!(obj instanceof javax.activation.DataSource))
                        return false;
                    Workbook wb = WorkbookFactory.create(new PushbackInputStream(((javax.activation.DataSource)obj).getInputStream()));
                    Sheet sheet = wb.getSheetAt(0);
                    assertEquals("Report", sheet.getSheetName());
                    assertEquals(cellValue, sheet.getRow(0).getCell(0).getStringCellValue());
                    return true;
                }catch(Exception e){
                    e.printStackTrace();
                    return false;
                }
            }

            public void appendTo(StringBuffer buffer) {
            }
        });

        return null;
    }

    public static InputStream checkDataSource2() {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object obj) {
                try{
                    if (!(obj instanceof javax.activation.DataSource))
                        return false;
                    Workbook wb = WorkbookFactory.create(
                            new PushbackInputStream(((javax.activation.DataSource)obj).getInputStream()));
                    Sheet sheet = wb.getSheetAt(0);
                    assertEquals("data1", sheet.getSheetName());
                    assertEquals("data1", sheet.getRow(0).getCell(0).getStringCellValue());
                    sheet = wb.getSheetAt(1);
                    assertEquals("data2", sheet.getSheetName());
                    assertEquals("data2", sheet.getRow(0).getCell(0).getStringCellValue());
                    return true;
                }catch(Exception e){
                    e.printStackTrace();
                    return false;
                }
            }

            public void appendTo(StringBuffer buffer) {
            }
        });

        return null;
    }
    
    private class WriteToFileHandler implements DataHandler {
        private final String filename;

        public WriteToFileHandler(String filename) {
            this.filename = filename;
        }

        public void handleData(Object data, DataContext context) {
            if (data==null)
                return;
            try {
                javax.activation.DataSource ds = (javax.activation.DataSource) data;
                File reportFile = new File(filename);
                reportFile.delete();
                FileOutputStream fos = new FileOutputStream(reportFile);
                try {
                    IOUtils.copy(ds.getInputStream(), fos);
                } finally {
                    fos.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }    
}
