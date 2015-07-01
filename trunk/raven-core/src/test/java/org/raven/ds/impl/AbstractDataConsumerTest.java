/*
 * Copyright 2014 Mikhail Titov.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.test.UserContextServiceModule;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractDataConsumerTest extends RavenCoreTestCase {
    private PushDataSource ds;
    private TestConsumer consumer;

    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder);
        builder.add(UserContextServiceModule.class);
    }
    
    @Before
    public void prepare() {
        ds = new PushDataSource();
        ds.setName("data source");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        consumer = new TestConsumer();
        consumer.setName("consumer");
        testsNode.addAndSaveChildren(consumer);
        consumer.setDataSource(ds);
        assertTrue(consumer.start());
    }
    
    @Test
    public void normalReceiveTest() {
        DataContext context = new DataContextImpl();
        IMocksControl mocks = DataContextImplTest.configureCallbacks(2, 1, context);
        mocks.replay();
        ds.pushData("test", context);
        ds.pushData(null, context);
        mocks.verify();
        assertArrayEquals(new Object[]{"test", null}, consumer.dataList.toArray());
    }
    
    @Test
    public void errorOnReceiveTest() {
        DataContext context = new DataContextImpl();
        IMocksControl mocks = DataContextImplTest.configureCallbacks(1, 1, context);
        mocks.replay();
        
        consumer.throwError = true;
        ds.pushData(null, context);
        assertTrue(context.hasErrors());
        mocks.verify();
        
        assertTrue(consumer.dataList.isEmpty());
    }
    
    public class TestConsumer extends AbstractDataConsumer {
        private List dataList = new LinkedList();
        private boolean throwError = false;
        
        @Override
        protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
            if (throwError)
                throw new Exception();
            dataList.add(data);
        }        
    }
}
