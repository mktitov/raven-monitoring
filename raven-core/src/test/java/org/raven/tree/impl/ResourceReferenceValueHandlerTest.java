/*
 * Copyright 2012 Mikhail Titov.
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
package org.raven.tree.impl;

import java.util.Locale;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.ResourceManager;
import org.raven.tree.ResourceManagerException;

/**
 *
 * @author Mikhail Titov
 */
public class ResourceReferenceValueHandlerTest extends RavenCoreTestCase {
    
    @Test
    public void test() throws ResourceManagerException, Exception {
        ResourceManager resourceManager = registry.getService(ResourceManager.class);
        assertNotNull(resourceManager);
        BaseNode res = new BaseNode("res");
        resourceManager.registerResource("test/res", Locale.getDefault(), res);
        
        BaseNode node = new BaseNode("test");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        NodeAttributeImpl attr = new NodeAttributeImpl("attr", Node.class, null, null);
        attr.setValueHandlerType(ResourceReferenceValueHandlerFactory.TYPE);
        attr.setOwner(node);
        attr.init();
        node.addNodeAttribute(attr);
        
        assertNull(attr.getValue());
        assertNull(attr.getRealValue());
        
        attr.setValue("test/res");
        assertSame(res, attr.getRealValue());
        assertEquals("test/res", attr.getValue());

        attr.setValue("test/res1");
        assertNull(attr.getRealValue());
        BaseNode res1 = new BaseNode("res1");
        resourceManager.registerResource("test/res1", Locale.getDefault(), res1);
        assertSame(res1, attr.getRealValue());
    }
}
