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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.junit.Test;
import org.raven.ds.InputStreamSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.XmlNodeBuilder;

/**
 *
 * @author Mikhail Titov
 */
public class InputStreamSourceImplTest extends RavenCoreTestCase {
    
    @Test
    public void sucessTest() throws Exception {
        XmlNodeBuilder builder = new XmlNodeBuilder("/test", createSource("src/test/conf/nodes_builder1.xml"));
        Node newNode = builder.build(testsNode);
        assertNotNull(newNode);
        assertSame(newNode, testsNode.getNode("testNode"));
        assertNotNull(newNode.getNode("testNode-1"));
    }
    
    @Test(expected=Exception.class)
    public void emptyTemplateTest() throws Exception {
        XmlNodeBuilder builder = new XmlNodeBuilder("/test", createSource("src/test/conf/nodes_builder2.xml"));
        builder.build(testsNode);
    }
    
    @Test(expected=Exception.class)
    public void moreThanOneNodeInTemplateTest() throws Exception {
        XmlNodeBuilder builder = new XmlNodeBuilder("/test", createSource("src/test/conf/nodes_builder3.xml"));
        builder.build(testsNode);
    }
    
    private InputStreamSource createSource(String path) throws FileNotFoundException {
        return new InputStreamSourceImpl(path, new FileInputStream(path));
    }
}
