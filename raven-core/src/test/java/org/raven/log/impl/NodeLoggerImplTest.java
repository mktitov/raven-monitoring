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

package org.raven.log.impl;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class NodeLoggerImplTest extends RavenCoreTestCase
{
    @Test
    public void serviceTest() throws Exception
    {
        NodeLogger nodeLogger = registry.getService(NodeLogger.class);
        assertNotNull(nodeLogger);
        assertNotNull(nodeLogger.getNodeLoggerNode());
    }

    @Test
    public void writeTest() throws Exception
    {
        NodeLogger nodeLogger = registry.getService(NodeLogger.class);
        
        JDBCConnectionPoolNode pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        tree.getRootNode().addChildren(pool);
        pool.save();
        pool.init();
        pool.setUserName("sa");
        pool.setPassword("");
        pool.setUrl(configurator.getConfig().getStringProperty(Configurator.TREE_STORE_URL, null));
        pool.setDriver("org.h2.Driver");
        pool.start();
        assertEquals(Status.STARTED, pool.getStatus());
        
        NodeLoggerNode nodeLoggerNode = nodeLogger.getNodeLoggerNode();
        nodeLoggerNode.setConnectionPool(pool);
        nodeLoggerNode.start();
        assertEquals(Status.STARTED, nodeLoggerNode.getStatus());
        
        ContainerNode node = new ContainerNode("nodeA");
        tree.getRootNode().addChildren(node);
        ContainerNode node2 = new ContainerNode("nodeB");
        tree.getRootNode().addChildren(node2);
        node.save();
        node.init();
        node2.save();
        node2.init();
        
        node.setLogLevel(LogLevel.DEBUG);
        node2.setLogLevel(LogLevel.DEBUG);

        List<NodeLogRecord> lst;
        Date td = new Date();
        Date fd = new Date(NodeLoggerImpl.addDays(td.getTime(), -1));
        lst = nodeLogger.getRecords(fd, td, node.getId(), LogLevel.DEBUG);
        
        node.debug("test debug {} {}", "1", "arg2");
        node2.debug("test debug {} {}", "1", "arg2");
        Thread.sleep(200);
        node.info("test info {} {}", "2", "arg2");
        node2.info("test info {} {}", "2", "arg2");
        Thread.sleep(200);
        node.warn("test warn {} {}", "3", "arg2");
        node2.warn("test warn {} {}", "3", "arg2");
        Thread.sleep(200);
        node.getLogger().error("test errorZ", new java.lang.IllegalArgumentException("testTh"));
        node2.error("test error {} {}", "4", "arg2");
        Thread.sleep(200);
        node.error("test error2 {} {}", "4", "arg2");
        Thread.sleep(1000);
        
        lst = nodeLogger.getRecords(fd, td, node.getId(), LogLevel.DEBUG);
        for(NodeLogRecord nl : lst)
        	System.out.println(nl);
    	System.out.println("---------------------");
        lst = nodeLogger.getRecords(fd, td, null, LogLevel.DEBUG);
        for(NodeLogRecord nl : lst)
        	System.out.println(nl);
        
        
    }
    
    
}