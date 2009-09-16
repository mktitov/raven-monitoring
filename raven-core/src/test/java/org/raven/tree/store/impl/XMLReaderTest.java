/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.tree.store.impl;

import java.io.FileInputStream;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class XMLReaderTest extends RavenCoreTestCase
{
    @Test
    public void testRead() throws Exception
    {
        FileInputStream is = new FileInputStream("src/test/conf/nodes1.xml");
        XMLReader reader = new XMLReader();
        reader.read(tree.getRootNode(), is);
        Node node1 = tree.getRootNode().getChildren("testNode-1");
        assertNotNull(node1);
        assertTrue(node1 instanceof ContainerNode);
        Node node1_1 = node1.getChildren("testNode-1-1");
        assertNotNull(node1_1);
        assertTrue(node1_1 instanceof ContainerNode);
        Node node2 = tree.getRootNode().getChildren("testNode-2");
        assertNotNull(node2);
        assertTrue(node2 instanceof ContainerNode);
    }
}