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
import org.junit.Test;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class MailWriterNodeTest extends RavenCoreTestCase
{
    @Test
    public void sendToGoogleTest() throws Exception
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        MailWriterNode email = new MailWriterNode();
        email.setName("email");
        tree.getRootNode().addAndSaveChildren(email);
        email.setDataSource(ds);
        email.setLogLevel(LogLevel.DEBUG);
        email.setSmptHost("smpt.gmail.com");
        email.setSmptPort(465);
        email.setUseAuth(Boolean.TRUE);
        email.setUseSsl(Boolean.TRUE);
        email.setUser("mikhail1207");
        email.setPassword("");
        email.setFrom("mikhail1207@gmail.com");
        email.setTo("mikhail1207@gmail.com");
        email.setSubject("Тестовое сообщение с файлом");
        email.setContentEncoding("utf-8");
        assertTrue(email.start());

        AttributeValueMessagePartNode part = new AttributeValueMessagePartNode();
        part.setName("part1");
        email.addAndSaveChildren(part);
        part.setContentType("text/plain");
        part.setValue("Привет мир");
        assertTrue(part.start());

        AttributeValueMessagePartNode part2 = new AttributeValueMessagePartNode();
        part2.setName("part2");
        email.addAndSaveChildren(part2);
        part2.setContentType("image/jpeg");
        part2.getNodeAttribute("value").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        part2.setValue("data");
        part2.setFileName("Фотография");
        assertTrue(part2.start());


        InputStream is = new FileInputStream("/home/tim/photo/80-400.jpeg");
        ds.pushData(is);
    }
}
