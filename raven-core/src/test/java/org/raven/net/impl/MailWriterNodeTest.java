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
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ViewableObjectImpl;

/**
 *
 * @author Mikhail Titov
 */
public class MailWriterNodeTest extends RavenCoreTestCase
{
    private MailWriterNode mailer;
    private PushDataSource ds;

    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        mailer = new MailWriterNode();
        mailer.setName("email");
        tree.getRootNode().addAndSaveChildren(mailer);
        mailer.setDataSource(ds);
        mailer.setLogLevel(LogLevel.DEBUG);

        mailer.setSmtpHost("mail.komi.mts.ru");
        mailer.setFrom("mikhail1207@gmail.com");
        mailer.setFromPersonalName("Генератор отчетов");
        mailer.setTo("tim@komi.mts.ru");
        mailer.setTo("mikhail1207@gmail.com");

//        mailer.setSmtpHost("smtp.gmail.com");
//        mailer.setSmtpPort(465);
//        mailer.setUseAuth(Boolean.TRUE);
//        mailer.setUseSsl(Boolean.TRUE);
//        mailer.setUser("mikhail1207");
//        mailer.setPassword("");
//        mailer.setFrom("mikhail1207@gmail.com");
//        mailer.setTo("mikhail1207@gmail.com");

        mailer.setSubject("Тестовое сообщение");
        assertTrue(mailer.start());
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

    @Test
    public void viewableObjectsTest() throws Exception
    {
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

        ViewableObjectsMessagePartNode part = new ViewableObjectsMessagePartNode();
        part.setName("part");
        mailer.addAndSaveChildren(part);
        part.setContentType("application/vnd.ms-excel");
        part.setFileName("report.xls");
        part.setSource(source);
        assertTrue(part.start());

        ds.pushData("marker");
    }
}
