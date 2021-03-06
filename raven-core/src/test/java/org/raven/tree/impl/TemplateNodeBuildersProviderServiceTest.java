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

import java.util.Set;
import org.apache.tapestry5.ioc.Configuration;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.ResourceDescriptor;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateNodeBuildersProviderServiceTest extends RavenCoreTestCase {
    
    public static void contributeTemplateNodeBuildersProvider(Configuration<ResourceDescriptor> descriptors) {
        descriptors.add(new ResourceDescriptorImpl("/org/raven/test-templates/", "group/node.xml"));
    }

    @Override
    protected void configureRegistry(Set<Class> builder) {
        super.configureRegistry(builder);
        builder.add(this.getClass());
    }

    @Test
    public void test() throws Exception {
        Node groupNode = tree.getNode("/Templates/group/");
        assertNotNull(groupNode);
        assertTrue(groupNode instanceof GroupNode);
        Node node = groupNode.getNode("node");
        assertNotNull(node);
        assertTrue(node instanceof ContainerNode);
        assertTrue(node.isStarted());
    }
}
