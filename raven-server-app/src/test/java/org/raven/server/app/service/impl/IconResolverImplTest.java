/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.server.app.service.impl;

import org.junit.Test;
import org.raven.ds.impl.AbstractDataPipe;
import org.raven.tree.impl.BaseNode;
import static org.junit.Assert.*;
/**
 *
 * @author Mikhail Titov
 */
public class IconResolverImplTest
{
    @Test()
    public void getPathTest()
    {
        IconResolverImpl resolver = new IconResolverImpl();
        assertEquals("org/raven/tree/impl/BaseNode.png", resolver.getPath(BaseNode.class));
        assertEquals("org/raven/tree/impl/BaseNode.png", resolver.getPath(TestNode.class));
        assertEquals("org/raven/ds/impl/AbstractDataPipe.png", resolver.getPath(AbstractDataPipe.class));
    }

    @Test
    public void getIconTest()
    {
        IconResolverImpl resolver = new IconResolverImpl();
        assertNotNull(resolver.getIcon(resolver.getPath(BaseNode.class)));
    }

    private static class TestNode extends BaseNode {
    }
}