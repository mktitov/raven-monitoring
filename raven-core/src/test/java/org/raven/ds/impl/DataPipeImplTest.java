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

package org.raven.ds.impl;

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.ds.impl.objects.TestDataConsumer2;
import org.raven.ds.impl.objects.TestDataSource2;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class DataPipeImplTest extends RavenCoreTestCase
{
    private DataPipeImpl pipe;
    private TestDataSource2 ds;
    private TestDataConsumer2 consumer;
    
    @Test
    public void simpleTest() throws Exception
    {
        createNodes();
        
        pipe.setData(null, "test");
        assertEquals("test", consumer.getData());
        pipe.setData(null, new Integer(1));
        assertEquals(new Integer(1), consumer.getData());
    }
    
    @Test
    public void convertToType() throws Exception
    {
        createNodes();
        
        NodeAttribute attr = pipe.getNodeAttribute(DataPipeImpl.CONVERT_VALUE_TO_TYPE_ATTRIBUTE);
        attr.setValue(Integer.class.getName());
        attr.save();
        
        pipe.setData(null, "1");
        assertEquals(1, consumer.getData());
    }
    
    @Test 
    public void expression() throws Exception
    {
        createNodes();
        
        NodeAttribute attr = pipe.getNodeAttribute(DataPipeImpl.EXPRESSION_ATTRIBUTE);
        attr.setValue("lastData+1");
        attr.save();
        
        pipe.setData(null, 1);
        assertEquals(2, consumer.getData());
    }
    
    @Test
    public void expression_with_convertToType() throws Exception
    {
        createNodes();
        NodeAttribute attr = pipe.getNodeAttribute(DataPipeImpl.CONVERT_VALUE_TO_TYPE_ATTRIBUTE);
        attr.setValue(Integer.class.getName());
        attr.save();
        attr = pipe.getNodeAttribute(DataPipeImpl.EXPRESSION_ATTRIBUTE);
        attr.setValue("lastData+1");
        attr.save();
        
        pipe.setData(null, "1");
        assertEquals(2, consumer.getData());
    }
    
    private void createNodes() throws Exception
    {
        ds = new TestDataSource2();
        ds.setName("ds");
        tree.getRootNode().addChildren(ds);
        ds.save();
        ds.init();
        
        pipe = new DataPipeImpl();
        pipe.setName("pipe");
        tree.getRootNode().addChildren(pipe);
        pipe.save();
        pipe.init();
        NodeAttribute attr = pipe.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
        attr.setValue(ds.getPath());
        attr.save();
        pipe.start();
        assertEquals(Status.STARTED, pipe.getStatus());
        
        consumer = new TestDataConsumer2();
        consumer.setName("consumer");
        tree.getRootNode().addChildren(consumer);
        consumer.save();
        consumer.init();
        attr = consumer.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
        attr.setValue(pipe.getPath());
        attr.save();
        consumer.setResetDataPolicy(AbstractDataConsumer.ResetDataPolicy.DONT_RESET_DATA);
        consumer.start();
        assertEquals(Status.STARTED, consumer.getStatus());
    }
    
}
