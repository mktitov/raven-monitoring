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

import groovy.lang.Closure;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.raven.ds.DataContext;
import org.raven.test.ServiceTestCase;
import org.raven.test.UserContextServiceModule;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataContextImplTest extends ServiceTestCase {
    private Node initiator;

    public static interface ClosureTester {
        public void executed();
    }
    
    public static Closure configureCallback(IMocksControl mocks, final int executeTimes) {
        final ClosureTester tester = mocks.createMock(ClosureTester.class);
        if (executeTimes > 0) {
            tester.executed();
            expectLastCall().times(executeTimes);
        }
        return new Closure(tester) {
            public void doCall() {
                if (executeTimes > 0)
                    tester.executed();
            }
        };
    }
    
    public static Closure configureOnEachCallback(IMocksControl mocks, final int executeTimes, DataContext context) {
        Closure callback = configureCallback(mocks, executeTimes);
        context.addCallbackOnEach(callback);
        return callback;
    }
    
    public static Closure configureOnEndCallback(IMocksControl mocks, final int executeTimes, DataContext context) {
        Closure callback = configureCallback(mocks, executeTimes);
        context.addCallbackOnEnd(callback);
        return callback;
    }
    
    public static IMocksControl configureCallbacks(int onEachExecutions, int onEndExecutions, DataContext context) {
        IMocksControl mocks = createControl();
        configureOnEachCallback(mocks, onEachExecutions, context);
        configureOnEndCallback(mocks, onEndExecutions, context);
        return mocks;
    }
    
    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder);
        builder.add(UserContextServiceModule.class);
    }
    
    @Before
    public void prepare() {
        
    }
    
    @Test
    public void executeOnEmptyCallbacksTest() {
        IMocksControl mocks = createMocks();
        mocks.replay();
        DataContextImpl context = new DataContextImpl();
//        context.executeCallbacksOnEnd(initiator, null);
        context.executeCallbacksOnEnd(initiator, null);
        context.executeCallbacksOnEach(initiator, null);
        mocks.verify();
    }
    
    @Test
    public void addCallbackTest() {
        IMocksControl mocks = createMocks();
        
        mocks.replay();
        
        final AtomicBoolean executed = new AtomicBoolean();
        Closure callback = new Closure(this) {
            public void doCall() {
                executed.set(true);
            }
        };
        final AtomicReference initiatorRef = new AtomicReference();
        Closure callback2 = new Closure(this) {
            public void doCall(Node initiator) {
                initiatorRef.set(initiator);
            }
        };
        
        DataContextImpl context = new DataContextImpl();
        context.addCallback(callback);
        context.addCallback(callback2);
        context.executeCallbacksOnEnd(initiator, null);
        assertTrue(executed.get());
        assertSame(initiator, initiatorRef.get());
        
        executed.set(false);
        context.executeCallbacksOnEnd(initiator, null);
        assertTrue(executed.get());
        
        mocks.verify();
    }
    
    @Test
    public void addCallbackOnEachTest() {
        IMocksControl mocks = createMocks();
        
        mocks.replay();
        
        final AtomicBoolean executed = new AtomicBoolean();
        Closure callback = new Closure(this) {
            public void doCall() {
                executed.set(true);
            }
        };
        
        DataContextImpl context = new DataContextImpl();
        context.addCallbackOnEach(callback);
        context.executeCallbacksOnEach(initiator, null);
        assertTrue(executed.get());
        
        mocks.verify();
    }
    
    private IMocksControl createMocks() {
        IMocksControl mocks = createControl();
        initiator = mocks.createMock(Node.class);        
        return mocks;
    }
    
}
