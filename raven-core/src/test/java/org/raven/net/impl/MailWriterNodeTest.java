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
package org.raven.net.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestScheduler;
import org.raven.cache.TemporaryFileManagerNode;
import org.raven.ds.impl.GzipContentTransformer;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.impl.ExecutorServiceNode;
import org.raven.table.TableImpl;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeError;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.TextNode;
import org.raven.tree.impl.ViewableObjectImpl;

/**
 *
 * @author Mikhail Titov
 */
public class MailWriterNodeTest extends RavenCoreTestCase
{
    private TestScheduler scheduler;
    private ExecutorServiceNode executor;
    private TemporaryFileManagerNode manager;
    private MailWriterNode mailer;
    private PushDataSource ds;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        executor = new ExecutorServiceNode();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        executor.setCorePoolSize(50);
        executor.setMaximumPoolSize(50);
        assertTrue(executor.start());

        manager = new TemporaryFileManagerNode();
        manager.setName("manager");
        testsNode.addAndSaveChildren(manager);
        manager.setDirectory("target/");
        manager.setScheduler(scheduler);
        
        assertTrue(manager.start());        
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        mailer = new MailWriterNode();
        mailer.setName("email");
        tree.getRootNode().addAndSaveChildren(mailer);
        mailer.setDataSource(ds);
        mailer.setLogLevel(LogLevel.DEBUG);

//        mailer.setSmtpHost("mail.spb.mts.ru");
        mailer.setSmtpHost("relay.spb.mts.ru");
        mailer.setFrom("it@komi.mts.ru");
        mailer.setFromPersonalName("Генератор отчетов");
        mailer.setTo("tim@komi.mts.ru");
//        mailer.setTo("mikhail1207@gmail.com");

//        mailer.setSmtpHost("smtp.gmail.com");
//        mailer.setSmtpPort(465);
//        mailer.setUseAuth(Boolean.TRUE);
//        mailer.setUseSsl(Boolean.TRUE);
//        mailer.setUser("mikhail1207");
//        mailer.setPassword("timtim357");
//        mailer.setFrom("mikhail1207@gmail.com");
//        mailer.setTo("mikhail1207@gmail.com");

        mailer.setSubject("Тестовое сообщение");
        assertTrue(mailer.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(mailer);
        assertTrue(collector.start());        
    }
    
//    @Test
    public void sendToGoogleTest() throws Exception
    {
        AttributeValueMessagePartNode part = new AttributeValueMessagePartNode();
        part.setName("part1");
        mailer.addAndSaveChildren(part);
        part.setContentType("text/plain");
        part.setValue("Привет мир");
        assertTrue(part.start());

        AttributeValueMessagePartNode part2 = new AttributeValueMessagePartNode();
        part2.setName("part2");
        mailer.addAndSaveChildren(part2);
        part2.setContentType("image/jpeg");
        part2.getNodeAttribute("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        part2.setValue("data");
        part2.setFileName("Фотография");
        assertTrue(part2.start());

        InputStream is = new FileInputStream("/home/tim/photo/80-400.jpeg");
        ds.pushData(is);
    }
    
//    @Test
    public void errorHandlerTest() throws Exception
    {
        mailer.setConnectionTimeout(500);
        mailer.setTo("invalid\temail\taddress");
        mailer.setUseErrorHandler(Boolean.TRUE);
        mailer.setErrorHandler("'error'");
        
        AttributeValueMessagePartNode part = new AttributeValueMessagePartNode();        
        part.setName("part1");
        mailer.addAndSaveChildren(part);
        part.setContentType("text/plain");
        part.setValue("Привет мир");
        assertTrue(part.start());

        ds.pushData("test");
        
        assertEquals(1, collector.getDataListSize());
        assertEquals("error", collector.getDataList().get(0));

        collector.getDataList().clear();
        mailer.setUseErrorHandler(Boolean.FALSE);
        ds.pushData("test");
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
    }

//    @Test
    public void sendToGoogleTestWithIfNode() throws Exception
    {
        IfNode if1 = new IfNode();
        if1.setName("if1");
        mailer.addAndSaveChildren(if1);
        if1.setUsedInTemplate(Boolean.FALSE);
        if1.getNodeAttribute("expression").setValue("data!=null");
        assertTrue(if1.start());

        AttributeValueMessagePartNode part = new AttributeValueMessagePartNode();
        part.setName("part1");
        if1.addAndSaveChildren(part);
        part.setContentType("text/plain");
        part.getNodeAttribute("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        part.setValue("data");
        assertTrue(part.start());

        IfNode if2 = new IfNode();
        if2.setName("if2");
        mailer.addAndSaveChildren(if2);
        if2.setUsedInTemplate(Boolean.FALSE);
        if2.getNodeAttribute("expression").setValue("data==null");
        assertTrue(if2.start());

        part = new AttributeValueMessagePartNode();
        part.setName("part1");
        if2.addAndSaveChildren(part);
        part.setContentType("text/plain");
        part.setValue("Этого сообщения не должно быть в письме");
        assertTrue(part.start());

        ds.pushData("Привет Мир!!!");
    }

//    @Test
    public void viewableObjectsTest() throws Exception
    {
        TestViewable source = createViewableObjectsSource();

        ViewableObjectsMessagePartNode part = new ViewableObjectsMessagePartNode();
        part.setName("part");
        mailer.addAndSaveChildren(part);
        part.setExecutor(executor);
        part.setContentType("application/vnd.ms-excel");
        part.setFileName("report.xls");
        part.setSource(source);
        assertTrue(part.start());

        ds.pushData("marker");
    }

//    @Test
    public void viewableObjectsWithTemporaryFileManagerTest() throws Exception
    {
        TestViewable source = createViewableObjectsSource();

        ViewableObjectsMessagePartNode part = new ViewableObjectsMessagePartNode();
        part.setName("part");
        mailer.addAndSaveChildren(part);
        part.setExecutor(executor);
        part.setTemporaryFileManager(manager);
        part.setUseTemporaryFileManager(Boolean.TRUE);
        part.setContentType("application/vnd.ms-excel");
        part.setFileName("report.xls");
        part.setSource(source);
        assertTrue(part.start());

        ds.pushData("marker");
    }

    @Test
    public void viewableObjectsWithTransformersTest() throws Exception
    {
        TestViewable source = createViewableObjectsSource();

        ViewableObjectsMessagePartNode part = new ViewableObjectsMessagePartNode();
        part.setName("part");
        mailer.addAndSaveChildren(part);
        part.setExecutor(executor);
        part.setTemporaryFileManager(manager);
        part.setUseTemporaryFileManager(Boolean.TRUE);
        part.setContentType("application/x-gzip");
        part.setFileName("report.xls.gz");
        part.setSource(source);        
        assertTrue(part.start());
        
        GzipContentTransformer gzip = new GzipContentTransformer();
        gzip.setName("gzip");
        part.addAndSaveChildren(gzip);
        assertTrue(gzip.start());

        ds.pushData("marker");
    }

    private TestViewable createViewableObjectsSource() throws NodeError {
        TestViewable source = new TestViewable();
        source.setName("source");
        tree.getRootNode().addAndSaveChildren(source);
        assertTrue(source.start());
        List<ViewableObject> vos = new ArrayList<ViewableObject>();
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<h1>Название отчета</h1>"));
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TEXT_MIMETYPE, "<b>01.01.2010</b>"));
        TableImpl table = new TableImpl(new String[]{"Колонка 1", "Колонка 2"});
        table.addRow(new Object[]{1, "знач 1"});
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
        source.setViewableObjects(vos);
        return source;
    }
}
