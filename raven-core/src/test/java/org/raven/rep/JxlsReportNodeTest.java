/*
 *  Copyright 2010 Mikhail Titov.
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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import java.io.PushbackInputStream;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.activation.DataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AttributeFieldValueGenerator;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class JxlsReportNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private JxlsReportNode report;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        report = new JxlsReportNode();
        report.setName("report");
        tree.getRootNode().addAndSaveChildren(report);
        report.setDataSource(ds);

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(report);
        assertTrue(collector.start());
    }

    public void testJxls() throws Exception
    {
        
    }

//    @Test
    public void test() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template.xls"));
        assertTrue(report.start());

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource(), isA(DataContext.class));
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

        AttributeFieldValueGenerator attrValue = new AttributeFieldValueGenerator();
        attrValue.setName("bean1");
        report.addAndSaveChildren(attrValue);
        attrValue.getNodeAttribute("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attrValue.setValue("data+' world'");

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource(), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("hello");

        verify(handler);
    }

//    @Test
    public void multiSheetReportTest() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template2.xls"));
        report.setMultiSheetReport(Boolean.TRUE);
        NodeAttribute attr = report.getNodeAttribute(JxlsReportNode.SHEET_NAME_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("data");
        assertTrue(report.start());

        AttributeFieldValueGenerator attrValue = new AttributeFieldValueGenerator();
        attrValue.setName("bean1");
        report.addAndSaveChildren(attrValue);
        attrValue.getNodeAttribute("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attrValue.setValue("data");

        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(checkDataSource2(), isA(DataContext.class));
        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("data1");
        ds.pushData("data2");
        ds.pushData(null);

        verify(handler);
    }

    @Test
    public void styleSelectorsTest() throws Exception
    {
        report.getReportTemplate().setDataStream(new FileInputStream("src/test/conf/jxls_template3.xls"));
        assertTrue(report.start());

        NodeAttribute attr;
        CellStyleSelectorNode selector = new CellStyleSelectorNode();
        selector.setName("selector1");
        report.addAndSaveChildren(selector);
        selector.setStyleCellLabel("style1");
        attr = selector.getNodeAttribute(CellStyleSelectorNode.SELECTOR_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("columnNumber%2>0 && !rowObject");
        assertTrue(selector.start());

        selector = new CellStyleSelectorNode();
        selector.setName("selector2");
        report.addAndSaveChildren(selector);
        selector.setStyleCellLabel("style2");
        attr = selector.getNodeAttribute(CellStyleSelectorNode.SELECTOR_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("columnNumber%2==0 && !rowObject");
        assertTrue(selector.start());

        AttributeFieldValueGenerator attrValue = new AttributeFieldValueGenerator();
        attrValue.setName("bean1");
        report.addAndSaveChildren(attrValue);
        attrValue.setValue("hello world");

        attrValue = new AttributeFieldValueGenerator();
        attrValue.setName("list");
        report.addAndSaveChildren(attrValue);
        attrValue.getNodeAttribute("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attrValue.setValue("[[name:'elem1', style:'listStyle1'], [name:'elem2', style:'listStyle2']]");

        selector = new CellStyleSelectorNode();
        selector.setName("selector3");
        report.addAndSaveChildren(selector);
        selector.setStyleCellLabel("style1");
        attr = selector.getNodeAttribute(CellStyleSelectorNode.SELECTOR_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("rowObject? true : false");

        attr = selector.getNodeAttribute(CellStyleSelectorNode.STYLE_CELL_LABEL_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("rowObject.style");
        assertTrue(selector.start());

//        DataHandler handler = createMock(DataHandler.class);
//        handler.handleData(checkDataSource(), isA(DataContext.class));
//        replay(handler);

        collector.setDataHandler(new DataHandler() {
            public void handleData(Object data, DataContext context) {
                try {
                    DataSource ds = (DataSource) data;
                    File reportFile = new File("target/report.xls");
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
        });
        ds.pushData("hello");

//        verify(handler);
    }

    public static InputStream checkDataSource()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object obj)
            {
                try{
                    if (!(obj instanceof DataSource))
                        return false;
                    Workbook wb = WorkbookFactory.create(new PushbackInputStream(((DataSource)obj).getInputStream()));
                    Sheet sheet = wb.getSheetAt(0);
                    assertEquals("Report", sheet.getSheetName());
                    assertEquals("hello world", sheet.getRow(0).getCell(0).getStringCellValue());
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

    public static InputStream checkDataSource2()
    {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object obj)
            {
                try{
                    if (!(obj instanceof DataSource))
                        return false;
                    Workbook wb = WorkbookFactory.create(new PushbackInputStream(((DataSource)obj).getInputStream()));
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
}