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

package org.raven.sql;

import java.sql.ResultSet;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.sql.objects.SqlDataConsumer;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class SQLDataSourceTest extends RavenCoreTestCase
{
    private JDBCConnectionPoolNode pool;
    private SQLDataSource sqlds;
    private SqlDataConsumer dataConsumer;
    
    @Test
    public void test() throws Exception
    {
        createNodes();
        sqlds.start();
        assertEquals(Status.STARTED, sqlds.getStatus());

        dataConsumer.setDataSource(sqlds);
        NodeAttribute attr = dataConsumer.getNodeAttribute(SQLDataSource.QUERY_ATTRIBUTE);
        assertNotNull(attr);
        attr.setValue("select count(*) from nodes");
        attr.save();
        attr = dataConsumer.getNodeAttribute(SQLDataSource.RESULT_TYPE_ATTRIBUTE);
        assertNotNull(attr);
        assertEquals(SQLDataSource.ResultType.TABLE, attr.getRealValue());
        dataConsumer.getNodeAttribute(AbstractDataSource.INTERVAL_ATTRIBUTE).setValue("1000");

        dataConsumer.start();
        assertEquals(Status.STARTED, dataConsumer.getStatus());

        Object data = dataConsumer.refereshData();
        assertNotNull(data);
        assertTrue(data instanceof ResultSet);

        attr.setValue(SQLDataSource.ResultType.SINGLE.toString());
        attr.save();

        data = dataConsumer.refereshData();
        assertNotNull(data);
        assertTrue(data instanceof Number);
    }

    private void createNodes() throws Exception
    {
        Config conf = configurator.getConfig();
        assertNotNull(conf);
        
        ConnectionPoolsNode poolsNode =
                (ConnectionPoolsNode) 
                tree.getNode(SystemNode.NAME).getChildren(ConnectionPoolsNode.NAME);
        assertNotNull(poolsNode);
        pool = new JDBCConnectionPoolNode();
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

        DataSourcesNode dataSources =
                (DataSourcesNode) tree.getNode(SystemNode.NAME).getChildren(DataSourcesNode.NAME);
        sqlds = new SQLDataSource();
        sqlds.setName("SQL datasource");
        dataSources.addChildren(sqlds);
        sqlds.save();
        sqlds.init();
        sqlds.setConnectionPool(pool);

        dataConsumer = new SqlDataConsumer();
        dataConsumer.setName("dataConsumer");
        tree.getRootNode().addChildren(dataConsumer);
        dataConsumer.save();
        dataConsumer.init();
        
    }
}
