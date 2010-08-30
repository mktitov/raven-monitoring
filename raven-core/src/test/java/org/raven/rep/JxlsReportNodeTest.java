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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import java.io.PushbackInputStream;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.activation.DataSource;
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

    @Test
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

    @Test
    public void recordTest() throws Exception
    {
        
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
}