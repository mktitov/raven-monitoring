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
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.FileNode;

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

    @Test
    public void testReadAttributes() throws Exception
    {
        FileInputStream is = new FileInputStream("src/test/conf/nodes2.xml");
        XMLReader reader = new XMLReader();
        reader.read(tree.getRootNode(), is);
        Node node = tree.getRootNode().getChildren("node");
        assertNotNull(node);
        assertTrue(node.start());
        
        checkAttributes(node);

        tree.reloadTree();

        node = tree.getNode(node.getPath());
        assertNotNull(node);

        checkAttributes(node);
    }

    @Test
    public void testDataFile() throws Exception
    {
        FileInputStream is = new FileInputStream("src/test/conf/nodes3.xml");
        XMLReader reader = new XMLReader();
        reader.read(tree.getRootNode(), is);
        Node node = tree.getRootNode().getChildren("node");
        assertNotNull(node);
        assertTrue(node instanceof FileNode);
        FileNode fileNode = (FileNode) node;
        InputStream input = fileNode.getFile().getDataStream();
        assertNotNull(input);
        String str = IOUtils.toString(input, "UTF-8");
//        byte[] arr = Base64.encodeBase64("Здравствуй Мир".getBytes());
//        System.out.println(new String(arr));
        assertEquals("Здравствуй Мир", str);
    }

    private void checkAttributes(Node node)
    {
        assertEquals(5, node.getNodeAttributes().size());

        NodeAttribute logLevel = node.getNodeAttribute(BaseNode.LOGLEVEL_ATTRIBUTE);
        assertNotNull(logLevel);
        LogLevel level = logLevel.getRealValue();
        assertEquals(LogLevel.DEBUG, level);

        NodeAttribute attr1 = node.getNodeAttribute("attr1");
        assertNotNull(attr1);
        assertEquals(String.class, attr1.getType());
        assertNull(attr1.getValue());
        assertNull(attr1.getDescription());
        assertNull(attr1.getValueHandlerType());
        assertFalse(attr1.isRequired());
        assertFalse(attr1.isTemplateExpression());

        NodeAttribute attr2 = node.getNodeAttribute("attr2");
        assertNotNull(attr2);
        assertEquals(Long.class, attr2.getType());
        assertTrue(attr2.isRequired());
        assertFalse(attr2.isTemplateExpression());
        assertEquals("Привет world!", attr2.getDescription());
        assertNull(attr2.getValueHandlerType());
        assertEquals(new Long(10), attr2.getRealValue());

        NodeAttribute attr3 = node.getNodeAttribute("attr3");
        assertNotNull(attr3);
        assertTrue(attr3.isTemplateExpression());


        NodeAttribute attr4 = node.getNodeAttribute("attr4");
        assertNotNull(attr4);
        assertEquals(ExpressionAttributeValueHandlerFactory.TYPE, attr4.getValueHandlerType());
        assertEquals(2, attr4.getRealValue());
    }
}