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

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.raven.TestScheduler;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.test.DataCollector;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class AttributeValueDataSourceNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.setValue("test");
        assertTrue(ds.start());

        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        assertEquals("test", ((List)collector.refereshData(null)).get(0));
    }
    
    @Test
    public void skipDataTest() throws Exception
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.getAttr("value").setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        ds.setValue("SKIP_DATA");
        assertTrue(ds.start());

        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        assertEquals(null, ((List)collector.refereshData(null)));
    }

    @Test
    public void dataStreamTest() throws Exception
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.getAttr(AttributeValueDataSourceNode.VALUE_ATTR)
                .setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        ds.setValue("dataStream << 1; 2");
        assertTrue(ds.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        collector.refereshData(null);
        assertArrayEquals(new Object[]{1,2}, collector.getDataList().toArray());
    }

    @Test
    public void dataConsumerAttributesTest() throws Exception
    {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        NodeAttribute valAttr = ds.getAttr("value");
        valAttr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        ds.setValue("'hello '+consAttr1+consAttr2");
        NodeAttributeImpl consAttr =
                new NodeAttributeImpl("consAttr1", String.class, "world", null);
        consAttr.setOwner(ds);
        consAttr.setValueHandlerType(DataConsumerAttributeValueHandlerFactory.TYPE);
        consAttr.save();
        consAttr.init();
        ds.addAttr(consAttr);
        NodeAttributeImpl consAttr2 =
                new NodeAttributeImpl("consAttr2", String.class, "world", null);
        consAttr2.setOwner(ds);
        consAttr2.setValueHandlerType(DataConsumerAttributeValueHandlerFactory.TYPE);
        consAttr2.save();
        consAttr2.init();
        ds.addAttr(consAttr2);
        ds.setRequiredAttributes("consAttr1");
        ds.setLogLevel(LogLevel.DEBUG);
        assertTrue(ds.start());


        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(ds);
        assertTrue(collector.start());

        NodeAttribute attr;
        attr = collector.getAttr("consAttr1");
        assertNotNull(attr);
        assertTrue(attr.isRequired());
        assertNull(attr.getValueHandlerType());
        attr.setValue("world");
        attr = collector.getAttr("consAttr2");
        assertNotNull(attr);
        assertNull(attr.getValueHandlerType());
        assertFalse(attr.isRequired());

        attr = new NodeAttributeImpl("consAttr2", String.class, "!", null);
        attr.setOwner(collector);
        attr.init();

        assertEquals("hello world!", ((List)collector.refereshData(Arrays.asList(attr))).get(0));
    }
    
    @Test
    public void noDataFoundTest() throws Exception {
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.getAttr(AttributeValueDataSourceNode.VALUE_ATTR).setValueHandlerType(
                ScriptAttributeValueHandlerFactory.TYPE);
        ds.setValue("'test'");
        assertTrue(ds.start());
        
        
        SafeDataPipeNode pipe = new SafeDataPipeNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        
        SafeDataConsumer collector = new SafeDataConsumer();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(pipe);
        assertTrue(collector.start());
        
        assertEquals("test", ((List)collector.refereshData(null)).get(0));
        
        pipe.getAttr("expression").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        pipe.setUseExpression(Boolean.TRUE);
        pipe.setExpression("data+'test'");
        assertEquals("testtest", ((List)collector.refereshData(null)).get(0));
    }
    
    @Test
    public void noDataFoundTest2() throws Exception {
        TestScheduler scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());
        
        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        ds.getAttr(AttributeValueDataSourceNode.VALUE_ATTR).setValueHandlerType(
                ScriptAttributeValueHandlerFactory.TYPE);
        ds.setValue("'test'");
        assertTrue(ds.start());
        
        
        SafeDataPipeNode pipe = new SafeDataPipeNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds);
        assertTrue(pipe.start());
        
        SchedulableDataPipe schedule = new SchedulableDataPipe();
        schedule.setName("schedule");
        tree.getRootNode().addAndSaveChildren(schedule);
        schedule.setScheduler(scheduler);
        schedule.setDataSource(pipe);
        assertTrue(schedule.start());
        
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(schedule);
        assertTrue(collector.start());
        
        schedule.executeScheduledJob(scheduler);
        
        assertEquals(1, collector.getDataListSize());
        assertEquals("test", collector.getDataList().get(0));
        
        pipe.getAttr("expression").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        pipe.setUseExpression(Boolean.TRUE);
        pipe.setExpression("data+'test'");
        
        collector.getDataList().clear();        
        schedule.executeScheduledJob(scheduler);
        assertEquals("testtest", collector.getDataList().get(0));
    }
}