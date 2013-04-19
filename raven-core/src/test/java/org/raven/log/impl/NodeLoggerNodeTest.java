/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.log.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.conf.Config;
import org.raven.conf.Configurator;
import org.raven.dbcp.impl.JDBCConnectionPoolNode;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.test.DataCollector;
import org.raven.test.ExecutorServiceWrapperNode;
import org.raven.test.InThreadExecutorService;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.SystemNode;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.raven.sched.ExecutorService;
import org.raven.sched.Task;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class NodeLoggerNodeTest extends RavenCoreTestCase {
    private NodeLoggerNode nodeLogger;
    private ExecutorServiceWrapperNode executorWrapper;
    private ExecutorService executorMock;
    
    @Before
    public void prepare() throws Exception {
        executorWrapper = new ExecutorServiceWrapperNode();
        executorWrapper.setName("executor wrapper");
        testsNode.addAndSaveChildren(executorWrapper);
        assertTrue(executorWrapper.start());
        
        InThreadExecutorService executor = new InThreadExecutorService();
        executor.setName("executor");
        testsNode.addAndSaveChildren(executor);
        assertTrue(executor.start());
        
        Config conf = configurator.getConfig();
        assertNotNull(conf);
        
        JDBCConnectionPoolNode pool = new JDBCConnectionPoolNode();
        pool.setName("pool");
        tree.getRootNode().addAndSaveChildren(pool);
        pool.setUserName(conf.getStringProperty(Configurator.TREE_STORE_USER, null));
        pool.setPassword(null);
        pool.setUrl(conf.getStringProperty(Configurator.TREE_STORE_URL, null));
        pool.setMinIdleTime(30000l);
        pool.setDriver("org.h2.Driver");
        pool.setConnectionProperties("prop1=value;prop2=value2");
        assertTrue(pool.start());
        
        nodeLogger = (NodeLoggerNode) tree.getRootNode()
                .getNode(SystemNode.NAME)
                .getNode(ServicesNode.NAME)
                .getNode(NodeLoggerNode.NAME);
        assertNotNull(nodeLogger);
        nodeLogger.setExecutor(executor);
        nodeLogger.setConnectionPool(pool);
        assertTrue(nodeLogger.start());
    }
    
    @Test
    public void messageFromOutsideLoggerNodeTest() {
        IMocksControl control = createMocks1();
        control.replay();
        
        executorWrapper.setWrapper(executorMock);
        nodeLogger.setExecutor(executorWrapper);
        nodeLogger.pushLogRecordToHandlers(createLogRec(testsNode, LogLevel.ERROR, "message"));
        
        control.verify();
    }
    
    @Test
    public void messageFromInsideLoggerNodeTest() {
        IMocksControl control = createMocks2();
        control.replay();
        
        executorWrapper.setWrapper(executorMock);
        nodeLogger.setExecutor(executorWrapper);
        nodeLogger.pushLogRecordToHandlers(createLogRec(createNode(nodeLogger, "test"), LogLevel.ERROR, "message"));
        
        
        control.verify();
    }
    
    @Test
    public void logLevelThresholdTest() {
        IMocksControl control = createMocks3();
        control.replay();
        
        executorWrapper.setWrapper(executorMock);
        nodeLogger.setExecutor(executorWrapper);
        nodeLogger.setLogLevelThreshold(LogLevel.WARN);
        nodeLogger.pushLogRecordToHandlers(createLogRec(testsNode, LogLevel.ERROR, "message"));
        nodeLogger.pushLogRecordToHandlers(createLogRec(testsNode, LogLevel.WARN, "message"));
        nodeLogger.pushLogRecordToHandlers(createLogRec(testsNode, LogLevel.DEBUG, "message"));
        nodeLogger.pushLogRecordToHandlers(createLogRec(testsNode, LogLevel.TRACE, "message"));
        
        control.verify();
    }
    
    @Test
    public void logHandlersTest() throws InterruptedException {
        DataCollector c1 = createCollector(nodeLogger, "c1");
        DataCollector c2 = createCollector(testsNode, "c2");
        nodeLogger.pushLogRecordToHandlers(createLogRec(testsNode, LogLevel.ERROR, "test mess"));
        assertEquals(1, c1.getDataListSize());
        assertEquals(0, c2.getDataListSize());
    }
    
    private DataCollector createCollector(Node owner, String name) {
        DataCollector collector = new DataCollector();
        collector.setName(name);
        owner.addAndSaveChildren(collector);
        collector.setDataSource(nodeLogger);
        assertTrue(collector.start());
        return collector;
    }
    
    private Node createNode(Node owner, String name) {
        BaseNode node = new BaseNode(name);
        owner.addAndSaveChildren(node);
        assertTrue(node.start());
        return node;
    }
    
    private IMocksControl createMocks1() {
        IMocksControl control = createControl();
        executorMock = control.createMock(ExecutorService.class);
        expect(executorMock.executeQuietly(isA(Task.class))).andReturn(Boolean.TRUE);
        return control;
    }
    
    private IMocksControl createMocks3() {
        IMocksControl control = createControl();
        executorMock = control.createMock(ExecutorService.class);
        expect(executorMock.executeQuietly(isA(Task.class))).andReturn(Boolean.TRUE).times(2);
        return control;
    }
    
    private IMocksControl createMocks2() {
        IMocksControl control = createControl();
        executorMock = control.createMock(ExecutorService.class);
        return control;
    }
    
    private static NodeLogRecord createLogRec(Node forNode, LogLevel level, String mess) {
        return new NodeLogRecord(forNode.getId(), forNode.getPath(), level, mess);
    }
}
