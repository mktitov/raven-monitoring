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

package org.raven.net;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.util.Properties;
import org.junit.Test;
import org.raven.DataCollector;
import org.raven.RavenCoreTestCase;
import org.raven.conf.Configurator;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
public class LdapReaderNodeTest extends RavenCoreTestCase
{
    @Test
    public void searchTest() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(System.getProperty("user.home")+"/raven/raven.cfg"));

        String ldapUrl = props.getProperty(Configurator.PROVIDER_URL);
        String baseDN = props.getProperty(Configurator.SEARCH_CONTEXT);
        String user = props.getProperty(Configurator.BIND_NAME);
        String pass = props.getProperty(Configurator.BIND_PASSWORD);

        LdapReaderNode reader = new LdapReaderNode();
        reader.setName("reader");
        tree.getRootNode().addAndSaveChildren(reader);
        reader.setUrl(ldapUrl);
        reader.setBaseDN(baseDN);
        reader.setUserDN(user);
        reader.setUserPassword(pass);
        reader.setLogLevel(LogLevel.DEBUG);
        assertTrue(reader.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(reader);
        assertTrue(collector.start());

        collector.refereshData(null);
        fail();
    }
}