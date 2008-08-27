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

package org.raven.dbcp;

import java.sql.Connection;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.SystemNode;

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

        pool.start();
        assertEquals(Status.STARTED, pool.getStatus());
        Connection connection = pool.getConnection();
        assertNotNull(connection);
        connection.close();
    }
}
