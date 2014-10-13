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

import org.junit.Before;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.raven.ds.DataContext;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceHelperTest {
    
    @Before
    public void prepare() {
        
    }
    
    @Test
    public void executeContextCallbacks() {
        Node initiator = createMock(Node.class);
        DataContext context = createMock(DataContext.class);
        context.executeCallbacksOnEach(initiator);
        replay(initiator, context);
        DataSourceHelper.executeContextCallbacks(initiator, context, "test");
        verify(initiator, context);
    }
    
    @Test
    public void executeContextCallbacks2() {
        Node initiator = createMock(Node.class);
        DataContext context = createMock(DataContext.class);
        context.executeCallbacksOnEach(initiator);
        context.executeCallbacksOnEnd(initiator);
        replay(initiator, context);
        DataSourceHelper.executeContextCallbacks(initiator, context, null);
        verify(initiator, context);
    }
}
