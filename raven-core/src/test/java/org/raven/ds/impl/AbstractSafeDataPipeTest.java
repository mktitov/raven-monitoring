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

package org.raven.ds.impl;

import groovy.lang.Closure;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import static org.junit.Assert.*;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractSafeDataPipeTest extends RavenCoreTestCase
{
    private TestSafeDataPipe pipe;
    private DataCollector c1, c2;

    @Before
    public void prepare()
    {
        pipe = new TestSafeDataPipe();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setLogLevel(LogLevel.DEBUG);
//        pipe.setDataSource(ds);
//        assertTrue(pipe.start());

        c1 = new DataCollector();
        c1.setName("c1");
        tree.getRootNode().addAndSaveChildren(c1);
        c1.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
        c1.setDataSource(pipe);
        assertTrue(c1.start());

        c2 = new DataCollector();
        c2.setName("c2");
        tree.getRootNode().addAndSaveChildren(c2);
        c2.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
        c2.setDataSource(pipe);
        assertTrue(c2.start());

    }

    @Test
    public void sendToOneConsumerTest()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        assertTrue(pipe.start());

        ds.addDataPortion("1");

        assertEquals("1", c1.refereshData(null));
        assertEquals(0, c2.getDataList().size());
    }

    @Test
    public void getDataImmediateWithNullConsumer() throws Exception
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        assertTrue(pipe.start());

        ds.addDataPortion("1");

        pipe.gatherDataForConsumer(null, new DataContextImpl());
        assertEquals(1, c1.getDataListSize());
        assertEquals(1, c2.getDataList().size());
    }

    @Test
    public void sendToAllConsumerTest()
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        assertTrue(pipe.start());

        ds.pushData("1");

        testCollector(c1, "1");
        testCollector(c2, "1");
    }

    @Test
    public void expressionTest()
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        pipe.setUseExpression(true);
        pipe.setExpression("dataSource.id+(data+1)");
        assertTrue(pipe.start());

        ds.pushData(1);
        testCollector(c1, ds.getId()+2);
    }

    @Test
    public void dataStreamTest()
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        pipe.setUseExpression(true);
        pipe.setExpression("dataStream << 1+data; 3");
        assertTrue(pipe.start());

        ds.pushData(1);

        assertEquals(2, c1.getDataListSize());
        assertEquals(2, c1.getDataList().get(0));
        assertEquals(3, c1.getDataList().get(1));
    }

    //forwardDataSourceAttributes==false
    @Test
    public void forwardDataSourceAttributesTest1()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());
        NodeAttribute attr = new NodeAttributeImpl("testAttr", String.class, null, null);
        ds.addConsumerAttribute(attr);

        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        assertNotNull(pipe.getNodeAttribute("testAttr"));

        c1.setDataSource(null);
        c1.setDataSource(pipe);
        assertNull(c1.getNodeAttribute("testAttr"));

        ds.addDataPortion("test");
        assertEquals("test", c1.refereshData(null));
        assertNotNull(ds.getLastSessionAttributes());
        assertNotNull(ds.getLastSessionAttributes().get("testAttr"));
    }

    //forwardDataSourceAttributes==true
    @Test
    public void forwardDataSourceAttributesTest2()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());
        NodeAttribute attr = new NodeAttributeImpl("testAttr", String.class, null, null);
        ds.addConsumerAttribute(attr);

        pipe.setForwardDataSourceAttributes(true);
        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        assertNull(pipe.getNodeAttribute("testAttr"));

        c1.setDataSource(null);
        c1.setDataSource(pipe);
        assertNotNull(c1.getNodeAttribute("testAttr"));

        ds.addDataPortion("test");
        assertEquals("test", c1.refereshData(null));
        assertNotNull(ds.getLastSessionAttributes());
        assertNotNull(ds.getLastSessionAttributes().get("testAttr"));
    }

    @Test
    public void sessionAttributesTest()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        assertTrue(pipe.start());

        TestSessionAttributeNode sessAttr = new TestSessionAttributeNode();
        sessAttr.setName("sessAttr");
        pipe.addAndSaveChildren(sessAttr);
        assertTrue(sessAttr.start());

        ds.addDataPortion("test");
        assertEquals("test", c1.refereshData(null));
        Map<String, NodeAttribute> attrs = ds.getLastSessionAttributes();
        assertNotNull(attrs);
        NodeAttribute attr = attrs.get("sessAttr");
        assertNotNull(attr);
        assertEquals(Integer.class, attr.getType());
        assertEquals(10, attr.getRealValue());
    }

    @Test
    public void generateConsumerAttributesFromSessionAttributesTest()
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        assertTrue(pipe.start());

        TestSessionAttributeNode sessAttr = new TestSessionAttributeNode();
        sessAttr.setName("sessAttr");
        pipe.addAndSaveChildren(sessAttr);
        assertTrue(sessAttr.start());
        NodeAttribute attr = new NodeAttributeImpl("consAttr", String.class, null, null);
        sessAttr.addConsumerAttribute(attr);

        c1.setDataSource(null);
        c1.setDataSource(pipe);

        assertNotNull(c1.getNodeAttribute("consAttr"));

        attr = new NodeAttributeImpl("consAttr1", String.class, null, null);
        ds.addConsumerAttribute(attr);
        pipe.setForwardDataSourceAttributes(true);

        c1.setDataSource(null);
        c1.setDataSource(pipe);

        assertNotNull(c1.getNodeAttribute("consAttr"));
        assertNotNull(c1.getNodeAttribute("consAttr1"));
    }

    @Test
    public void preprocessTest() throws Exception
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        pipe.setUsePreProcess(true);
        pipe.setPreProcess(
                "sessAttrs['sessAttr'].realValue+sessAttrs['consAttr'].realValue");
        assertTrue(pipe.start());

        TestSessionAttributeNode sessAttr = new TestSessionAttributeNode();
        sessAttr.setName("sessAttr");
        pipe.addAndSaveChildren(sessAttr);
        assertTrue(sessAttr.start());

        NodeAttribute attr = new NodeAttributeImpl("consAttr", Integer.class, 1, null);
        attr.init();
        Object data = c1.refereshData(Arrays.asList(attr));
        assertEquals(11, data);
    }

    @Test
    public void skipDataTest() throws Exception
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        pipe.setDataSource(ds);
        pipe.setUseExpression(true);
        pipe.setExpression("data==1? SKIP_DATA : data");
        assertTrue(pipe.start());

        ds.pushData(1);
        ds.pushData(2);
        testCollector(c1, 2);
        
    }

    @Test
    public void autoLinkDataSourceTest() throws Exception
    {
        ContainerNode container = new ContainerNode("container");
        tree.getRootNode().addAndSaveChildren(container);
        assertTrue(container.start());

        TestSafeDataPipe[] pipes = new TestSafeDataPipe[3];
        for (int i=0; i<3; ++i){
            pipes[i] = new TestSafeDataPipe();
            pipes[i].setName("p"+i);
            container.addAndSaveChildren(pipes[i]);
            assertNull(pipes[i].getDataSource());
        }
        pipes[2].setAutoLinkDataSource(true);
        assertEquals(pipes[1], pipes[2].getDataSource());
        assertNull(pipes[1].getDataSource());
        pipes[0].setAutoLinkDataSource(true);
        assertNull(pipes[0].getDataSource());
        pipes[1].getAttr(AbstractDataConsumer.AUTO_LINK_DATA_SOURCE_ATTR).setValue("true");
        assertEquals(pipes[0], pipes[1].getDataSource());
    }
    
    @Test
    public void callbacksOnNoConsumersTest() {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        DataContextImpl context = new DataContextImpl();
        final AtomicReference initiatorRef = new AtomicReference();
        context.addCallback(new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef.set(initiator);
            }
        });
        final AtomicReference initiatorRef2 = new AtomicReference();
        context.addCallbackOnEach(new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef2.set(initiator);
            }
        });
        
        c1.stop();
        c2.stop();
        ds.pushData("test", context);
        assertSame(pipe, initiatorRef2.get());
        assertNull(initiatorRef.get());
        
        initiatorRef2.set(null);
        ds.pushData(null, context);
        assertSame(pipe, initiatorRef2.get());
        assertSame(pipe, initiatorRef.get());        
    }
    
    @Test
    public void callbackOnDoSetDataError() throws Exception {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        DataContextImpl context = new DataContextImpl();
        final AtomicReference initiatorRef = new AtomicReference();
        context.addCallback(new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef.set(initiator);
            }
        });
        final AtomicReference initiatorRef2 = new AtomicReference();
        context.addCallbackOnEach(new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef2.set(initiator);
            }
        });
        
        pipe.setThrowErrorOnSetData(true);
        ds.pushData("test", context);
        assertSame(pipe, initiatorRef2.get());
        assertNull(initiatorRef.get());
        
        initiatorRef2.set(null);
        ds.pushData(null, context);
        assertSame(pipe, initiatorRef2.get());
        assertSame(pipe, initiatorRef.get());        
    }
    
    @Test
    public void noCallbackOnNormalDataProcessing() throws Exception {
        PushDataSource ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        DataContextImpl context = new DataContextImpl();
        final AtomicReference initiatorRef = new AtomicReference();
        context.addCallback(new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef.set(initiator);
            }
        });
        final AtomicReference initiatorRef2 = new AtomicReference();
        context.addCallbackOnEach(new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef2.set(initiator);
            }
        });
        
        c2.stop();
        ds.pushData("test", context);
        assertSame(c1, initiatorRef2.get());
        assertNull(initiatorRef.get());
        
        initiatorRef2.set(null);
        ds.pushData(null, context);
        assertSame(c1, initiatorRef2.get());
        assertSame(c1, initiatorRef.get());
    }

    private void testCollector(DataCollector collector, Object value)
    {
        assertEquals(1, collector.getDataList().size());
        assertEquals(value, collector.getDataList().get(0));
    }
}