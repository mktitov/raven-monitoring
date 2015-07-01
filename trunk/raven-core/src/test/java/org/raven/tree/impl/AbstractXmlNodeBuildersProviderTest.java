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
package org.raven.tree.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeBuilder;
import org.raven.tree.ResourceDescriptor;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractXmlNodeBuildersProviderTest extends RavenCoreTestCase {
    private final static String templatesBase = "/org/raven/test-templates/";
    
    @Test
    public void test() throws Exception {
        List<ResourceDescriptor> descs = new LinkedList<ResourceDescriptor>();
        descs.add(new ResourceDescriptorImpl(templatesBase, "test-template.xml"));
        XmlNodeBuildersProvider provider = new XmlNodeBuildersProvider(descs);
        Collection<NodeBuilder> builders = provider.getNodeBuilders();
        assertNotNull(builders);
        assertEquals(1, builders.size());
        assertEquals(testsNode.getPath(), provider.getBasePath());
        NodeBuilder builder = builders.iterator().next();
        Node node = builder.build(testsNode);
        assertNotNull(node);
        assertSame(node, testsNode.getNode("testNode"));
    }
    
    private class XmlNodeBuildersProvider extends AbstractXmlNodeBuildersProvider {
        public XmlNodeBuildersProvider(Collection<ResourceDescriptor> resources) {
            super(resources, testsNode.getPath());
        }

        public Node createPathNode() {
            return new GroupNode();
        }
    }
}
