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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tapestry5.ioc.Configuration;
import org.junit.*;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.*;

/**
 *
 * @author Mikhail Titov
 */
public class ResourceManagerImplTest extends RavenCoreTestCase {
    
    private ResourceManager manager;
    
    public static void contributeResourceManager(Configuration conf) {
        conf.add(new TestResourceRegistrator());
    }

    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder);
        builder.add(ResourceManagerImplTest.class);
    }
    
    @Before
    public void prepare() {
        manager = registry.getService(ResourceManager.class);
    }
    
    @Test
    public void serviceTest() {
        ResourceManager manager = registry.getService(ResourceManager.class);
        assertNotNull(manager);
    }
    
    @Test
    public void registartorTest() throws Exception {
        assertFalse(manager.containsResource("test1", Locale.ENGLISH));
        assertFalse(manager.containsResource("test", Locale.FRENCH));
        assertTrue(manager.containsResource("test bundle/test", Locale.ENGLISH));
        
        Node node = manager.getResource("test bundle/test", Locale.ENGLISH);
        assertNotNull(node);
        NodeAttribute attr = node.getNodeAttribute("attr");
        assertNotNull(attr);
        assertEquals("value", attr.getValue());
        attr.setValue("value2");
        attr.save();
    }
    
    @Test
    public void registerResourceTest() {
        Node res = new BaseNode("test");
        try {
            manager.registerResource("test", Locale.ENGLISH, res);
            fail();
        } catch (ResourceManagerException ex) {
        }
    }
    
    @Test
    public void registerResource2Test() {
        Node res = new BaseNode("test");
        try {
            manager.registerResource("test bundle/test/"+Locale.ENGLISH.toString(), Locale.ENGLISH, res);
            fail();
        } catch (ResourceManagerException ex) {
        }
    }
    
    @Test
    public void registerResource3Test() {
        Node res = new BaseNode("test");
        try {
            manager.registerResource("test bundle/test/test", Locale.ENGLISH, res);
            fail();
        } catch (ResourceManagerException ex) {
        }
    }
    
    @Test
    public void registerResource4Test() throws Exception{
        Node res = new BaseNode("test");
        assertTrue(manager.registerResource("test bundle/test", Locale.FRENCH, res));
    }
    
    @Test
    public void getKeyForResourceTest() {
        Node node = manager.getResource("test bundle/test", Locale.ENGLISH);
        assertNotNull(node);
        assertEquals("\"test bundle\"/\"test\"/", manager.getKeyForResource(node));
    }
    
    @Test
    public void getKeyForResource2Test() {
        Node node = manager.getResource("test bundle/test", Locale.ENGLISH);
        assertNotNull(node);
        assertEquals("\"test bundle\"/\"test\"/", manager.getKeyForResource(node.getParent()));
    }
    
    @Test
    public void getResourceTest() {
        Node bundle = manager.getResource("test bundle", Locale.ENGLISH);
        assertNotNull(bundle);
        Node res = manager.getResource(bundle, "test", Locale.ENGLISH);
        assertNotNull(res);
        NodeAttribute attr = res.getNodeAttribute("attr");
        assertNotNull(attr);
    }
    
    @Test
    public void getResourceTest2() {
        ContainerNode container = new ContainerNode("container");
        tree.getRootNode().addAndSaveChildren(container);
        assertTrue(container.start());
        
        BaseNode res = new BaseNode("res");
        container.addAndSaveChildren(res);
        assertTrue(res.start());
        
        Node node = manager.getResource(container, "res", null);
        assertSame(res, node);
    }
    
    private static class TestResourceRegistrator implements ResourceRegistrator {
        public void registerResources(ResourceManager resourceManager) {
            try {
                Node testRes = new BaseNode("test1");
                NodeAttributeImpl attr = new NodeAttributeImpl("attr", String.class, "value", null);
                attr.setOwner(testRes);
                attr.init();
                testRes.addNodeAttribute(attr);
                assertTrue(resourceManager.registerResource("test bundle/test", Locale.ENGLISH, testRes));
            } catch (Exception ex) {
                Logger.getLogger(ResourceManagerImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
