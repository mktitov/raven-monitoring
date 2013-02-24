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
package org.raven.auth.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class FilterIpByExpressionTest extends RavenCoreTestCase {
    
    private FilterIpByExpression filter;
    private NodeAttribute filterAttr;
    
    @Before
    public void prepare() {
        filter = new FilterIpByExpression();
        filter.setName("filter");
        testsNode.addAndSaveChildren(filter);
        filterAttr = filter.getAttr(FilterIpByExpression.FILTER_ATTR);
        assertTrue(filter.start());
    }
    
    @Test
    public void ipTest() throws Exception {
        filterAttr.setValue("host.ip=='1.1.1.1'");
        assertTrue(filter.isIpAllowed("1.1.1.1"));
        assertFalse(filter.isIpAllowed("2.1.1.1"));
    }
    
    @Test
    public void hostTest() throws Exception {
        filterAttr.setValue("host.name=='localhost'");
        assertTrue(filter.isIpAllowed("127.0.0.1"));
        filterAttr.setValue("host.name=='_localhost'");
        assertFalse(filter.isIpAllowed("127.0.0.1"));
    }
}
