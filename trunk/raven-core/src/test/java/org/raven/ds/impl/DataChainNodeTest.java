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
package org.raven.ds.impl;

import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class DataChainNodeTest extends RavenCoreTestCase {
    private PushDataSource ds;
    private DataChainNode chain;
    private SafeDataPipeNode chainPipe;
    private DataCollector chainCollector;
    private DataCollector collector;
    private DataChainStubNode stub;
    
    @Before
    public void prepare() {
        ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        chain = new DataChainNode();
        chain.setName("chain");
        testsNode.addAndSaveChildren(chain);
        chain.setDataSource(ds);
        assertTrue(chain.start());
        
        chainPipe = new SafeDataPipeNode();
        chainPipe.setName("chain pipe");
        chain.addAndSaveChildren(chainPipe);
        chainPipe.setDataSource(chain);
        chainPipe.setUseExpression(Boolean.TRUE);
        chainPipe.setExpression("data+10");
        assertTrue(chainPipe.start());
        
        stub = new DataChainStubNode();
        stub.setName("stub");
        chain.addAndSaveChildren(stub);
        assertTrue(stub.start());
        assertSame(chainPipe, stub.getDataSource());
//        chainCollector = new DataCollector();
//        chainCollector.setName("chain collector");
//        chain.addAndSaveChildren(chainCollector);
//        chainCollector.setDataSource(chainPipe);
//        assertTrue(chainCollector.start());
        
        
        collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(chain);
        assertTrue(collector.start());
    }
    
    @Test
    public void test() {
        ds.pushData(1);
        assertEquals(1, collector.getDataListSize());
        assertEquals(11, collector.getDataList().get(0));        
    }
    
    @Test
    public void useChainFalse() {
        chain.setUseChain(false);
        ds.pushData(1);
        assertEquals(1, collector.getDataListSize());
        assertEquals(1, collector.getDataList().get(0));        
    }
    
    @Test
    public void useChainExpressionTest() throws Exception {
        NodeAttribute attr = chain.getAttr("useChain");
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("data==1");
        ds.pushData(1);
        ds.pushData(2);
        assertEquals(2, collector.getDataListSize());
        assertEquals(11, collector.getDataList().get(0));        
        assertEquals(2, collector.getDataList().get(1));        
    }
    
}