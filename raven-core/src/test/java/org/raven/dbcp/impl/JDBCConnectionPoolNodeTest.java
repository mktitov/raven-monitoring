/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.dbcp.impl;

import java.sql.Connection;
import org.junit.Test;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class JDBCConnectionPoolNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        Config conf = configurator.getConfig();
        assertNotNull(conf);
        
        JDBCConnectionPoolNode pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        pool.setUserName(conf.getStringProperty(Configurator.TREE_STORE_USER, null));
        pool.setPassword(null);
        pool.setUrl(conf.getStringProperty(Configurator.TREE_STORE_URL, null));
//        pool.setUrl("jdbc:h2:tcp://localhost/~/documents/h2/test2");
        pool.setMinIdleTime(30000l);
        pool.setDriver("org.h2.Driver");
        pool.setConnectionProperties("prop1=value;prop2=value2");
        assertTrue(pool.start());
        Connection connection = pool.getConnection();
        assertNotNull(connection);
        connection.close();
    }
}
