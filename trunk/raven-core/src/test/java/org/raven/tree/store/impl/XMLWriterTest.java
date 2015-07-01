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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.raven.log.LogLevel;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.FileNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class XMLWriterTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        XMLWriter writer = new XMLWriter();
        FileOutputStream out = new FileOutputStream("target/nodes.xml", false);

        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        node.setLogLevel(LogLevel.TRACE);
        assertTrue(node.start());

        FileNode fileNode = new FileNode();
        fileNode.setName("fileNode");
        node.addAndSaveChildren(fileNode);
        ByteArrayInputStream data = new ByteArrayInputStream("Привет world".getBytes("UTF-8"));
        fileNode.getFile().setDataStream(data);
        fileNode.getFile().setFilename("hello.txt");
        
        NodeAttribute attr = new NodeAttributeImpl(
                "attr1", String.class, "attr1 value", "attr1 description");
        attr.setOwner(fileNode);
        attr.init();
        attr.setRequired(true);
        MessageComposer composer = new MessageComposer(registry.getService(MessagesRegistry.class));
        composer
                .append("Привет ")
                .append("message:org.raven.tree.store.impl.TestMessages:worldMessage");
        attr.setDescriptionContainer(composer);
        fileNode.addNodeAttribute(attr);
        attr.save();

        attr = new NodeAttributeImpl("attr2", String.class, null, null);
        attr.setOwner(fileNode);
        attr.init();
        attr.setTemplateExpression(true);
        fileNode.addNodeAttribute(attr);
        attr.save();
        
        writer.write(out, "UTF-8", node);
        out.close();

        //check
        tree.remove(node);
        tree.reloadTree();
        assertNull(tree.getRootNode().getChildren("node"));
        XMLReader reader = new XMLReader();
        reader.read(tree.getRootNode(), new FileInputStream("target/nodes.xml"));

        node =  (ContainerNode) tree.getRootNode().getChildren("node");
        assertNotNull(node);
        assertEquals(LogLevel.TRACE, node.getLogLevel());

        fileNode = (FileNode) node.getChildren("fileNode");
        assertNotNull(fileNode);
        assertEquals(fileNode.getFile().getFilename(), "hello.txt");
        InputStream fileData = fileNode.getFile().getDataStream();
        assertNotNull(fileData);
        String decodedData = IOUtils.toString(fileData, "UTF-8");
        assertEquals("Привет world", decodedData);

        attr = fileNode.getNodeAttribute("attr1");
        assertNotNull(attr);
        assertEquals(String.class, attr.getType());
        assertTrue(attr.isRequired());
        assertEquals("attr1 value", attr.getValue());
        assertEquals("Привет world!", attr.getDescription());
        assertFalse(attr.isTemplateExpression());

        attr = fileNode.getNodeAttribute("attr2");
        assertNotNull(attr);
        assertTrue(attr.isTemplateExpression());
    }
}