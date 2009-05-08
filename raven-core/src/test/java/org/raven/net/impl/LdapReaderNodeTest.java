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

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import org.junit.Ignore;
import org.junit.Test;
import org.raven.DataCollector;
import org.raven.RavenCoreTestCase;
import org.raven.RavenUtils;
import org.raven.conf.Configurator;
import org.raven.ds.impl.AbstractDataConsumer.ResetDataPolicy;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.Table;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
@Ignore
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
        collector.setResetDataPolicy(ResetDataPolicy.DONT_RESET_DATA);
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.FILTER_ATTRIBUTE));
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.ATTRIBUTES_ATTRIBUTE));
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.FETCH_ATTRIBUTES_ATTRIBUTE));
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.START_SEARCH_FROM_ATTRIBUTE));
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.ADD_OBJECTDN_TO_RESULT_ATTRIBUTE));
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.USE_ROW_FILTER_ATTRIBUTE));
        assertNotNull(collector.getNodeAttribute(LdapReaderNode.ROW_FILTER_ATTRIBUTE));
        collector.getNodeAttribute(LdapReaderNode.FILTER_ATTRIBUTE).setValue(
                "(&(objectCategory=CN=Person,CN=Schema,CN=Configuration,DC=NW,DC=MTS,DC=RU)" +
                "(mobile=91286729*))");
        collector.getNodeAttribute(LdapReaderNode.START_SEARCH_FROM_ATTRIBUTE).setValue(
                "OU=Блок ИТ," +
                "OU=Коми,OU=MTSNWUSER,DC=USR,DC=NW,DC=MTS,DC=RU");
        collector.getNodeAttribute(LdapReaderNode.ATTRIBUTES_ATTRIBUTE).setValue("mobile");
        collector.getNodeAttribute(LdapReaderNode.FETCH_ATTRIBUTES_ATTRIBUTE).setValue("true");
        collector.getNodeAttribute(
                LdapReaderNode.ADD_OBJECTDN_TO_RESULT_ATTRIBUTE).setValue("true");
        collector.getNodeAttribute(LdapReaderNode.USE_ROW_FILTER_ATTRIBUTE).setValue("true");
        NodeAttribute filterAttr = collector.getNodeAttribute(LdapReaderNode.ROW_FILTER_ATTRIBUTE);
        filterAttr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        filterAttr.setValue("row['mobile']==~/[0-9]+/");
        assertTrue(collector.start());

        Object obj = collector.refereshData(null);
        assertNotNull(obj);
        assertTrue(obj instanceof Table);
        List<Object[]> rows = RavenUtils.tableAsList((Table)obj);
        assertNotNull(rows);
        assertEquals(4, rows.size());
    }
}