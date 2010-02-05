/*
 *  Copyright 2008 Mikhail Titov.
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
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.ServiceTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.store.TreeStoreError;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class H2TreeStoreTest extends ServiceTestCase
{
    private static H2TreeStore store;

    @Before
    public void initTests() throws TreeStoreError
    {
        store = new H2TreeStore();
        store.init("jdbc:h2:tcp://localhost/~/test;", "sa", "");
    }
    
    @Test
    public void saveAndLoad() throws TreeStoreError, SQLException
    {
        store.removeNodes();
        
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);
        
        Node loadedNode = store.getNode(node.getId());
        assertNotNull(loadedNode);
        assertEquals("node", loadedNode.getName());
        assertEquals(node.getId(), loadedNode.getId());
        assertNull(loadedNode.getParent());
        
        loadedNode.setName("node 1");
        
        store.saveNode(loadedNode);
        
        assertEquals(node.getId(), loadedNode.getId());
        
        Node updatedNode = store.getNode(loadedNode.getId());
        assertNotNull(updatedNode);
        assertEquals("node 1", updatedNode.getName());
        assertEquals(node.getId(), updatedNode.getId());
        assertNull(updatedNode.getParent());
        
        store.removeNode(updatedNode.getId());
        
        Node removedNode = store.getNode(updatedNode.getId());
        assertNull(removedNode);
    }

    @Test
    public void saveAndLoadAttributes() throws Exception
    {
        MessagesRegistry messagesRegistry = registry.getService(MessagesRegistry.class);

        store.removeNodes();
        
        ContainerNode node = new ContainerNode();
        node.setName("node");
        
        store.saveNode(node);
        
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setDisplayName("display name");
        attr.setDescription("test");
        attr.setOwner(node);
        attr.setParameterName("parameterName");
        attr.setParentAttribute("parentAttribute");
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setValueHandlerType("valueHandlerType");
        attr.setTemplateExpression(true);
        
        store.saveNodeAttribute(attr);
        
        assertTrue(attr.getId()>0);
        
        ContainerNode node1 = (ContainerNode) store.getNode(node.getId());
        assertNotNull(node1);
        
        assertNotNull(node1.getNodeAttributes());
        assertEquals(1, node1.getNodeAttributes().size());
        
        NodeAttributeImpl attr1 = (NodeAttributeImpl) node1.getNodeAttribute("name");
        assertNotNull(attr1);
        assertEquals(attr.getId(), attr1.getId());
        assertSame(node1, attr1.getOwner());
        assertEquals("display name", attr1.getDisplayName());
        assertEquals("parameterName", attr1.getParameterName());
        assertEquals("parentAttribute", attr1.getParentAttribute());
        assertEquals(String.class, attr1.getType());
        assertEquals("value", attr1.getRawValue());
        assertFalse(attr1.isRequired());
        assertTrue(attr1.isTemplateExpression());
        assertEquals("valueHandlerType", attr1.getValueHandlerType());
        
        attr1.setDescription("description1");
        attr1.setName("name1");
        attr1.setDisplayName("display name1");
        attr1.setOwner(node);
        attr1.setParameterName("parameterName1");
        attr1.setParentAttribute("parentAttribute1");
        attr1.setType(Integer.class);
        attr1.setRawValue("1");
        attr1.setRequired(true);
        attr1.setTemplateExpression(false);
        attr1.setValueHandlerType("changedValueHandlerType");
        
        store.saveNodeAttribute(attr1);
        
        node1 = (ContainerNode) store.getNode(node.getId());
        assertNotNull(node1);
        assertNotNull(node1.getNodeAttributes());
        assertEquals(1, node1.getNodeAttributes().size());
        
        attr1 = (NodeAttributeImpl) node1.getNodeAttribute("name1");
        assertNotNull(attr1);
        assertEquals("display name1", attr1.getDisplayName());
        
        store.removeNodeAttribute(attr1.getId());
        node1 = (ContainerNode) store.getNode(node.getId());
        assertNotNull(node1);
        assertTrue(node1.getNodeAttributes().isEmpty());
    }
    
    @Test
    public void saveAndLoadAttributes2() throws Exception
    {
        MessagesRegistry messagesRegistry = registry.getService(MessagesRegistry.class);

        store.removeNodes();

        ContainerNode node = new ContainerNode();
        node.setName("node");

        store.saveNode(node);

        MessageComposer composer = new MessageComposer(messagesRegistry);
        composer.append("message:1.2.3.4:test").append("dd.MM.yyyy HH:mm").append("</p>");
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setDescriptionContainer(composer);
        attr.setOwner(node);
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setValueHandlerType("valueHandlerType");
        attr.setTemplateExpression(true);

        store.saveNodeAttribute(attr);

        NodeAttributeImpl attrClone = (NodeAttributeImpl) attr.clone();
    }

    @Test
    public void getRootNode() throws Exception
    {
        store.removeNodes();
        
        ContainerNode rootNode = new ContainerNode("root");
        store.saveNode(rootNode);
        
        ContainerNode childNode = new ContainerNode("child");
        childNode.setIndex(1);
        rootNode.addChildren(childNode);
        
        store.saveNode(childNode);
        
        Node node = store.getRootNode();
        assertNotNull(node);
        
        assertNotNull(node.getChildrens());
        assertEquals(1, node.getChildrens().size());
        
        Node node2 = node.getChildren("child");
        assertNotNull(node2);
        
    }

    @Test
    public void hasNodeAttributeBinaryData() throws SQLException
    {
        store.removeNodes();
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);
        
        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setOwner(node);
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setTemplateExpression(true);
        store.saveNodeAttribute(attr);

        assertFalse(store.hasNodeAttributeBinaryData(attr));

        Connection con = store.getConnection();
        Statement st = con.createStatement();
        st.executeUpdate(
                String.format("insert into %s (id) values(%d)"
                , H2TreeStore.NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME, attr.getId()));
        con.commit();

        assertTrue(store.hasNodeAttributeBinaryData(attr));
    }

    @Test
    public void getNodeAttributeBinaryData() throws SQLException, IOException
    {
        store.removeNodes();
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);

        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setOwner(node);
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setTemplateExpression(true);
        store.saveNodeAttribute(attr);

        Connection con = store.getConnection();
        PreparedStatement st = con.prepareStatement(String.format(
                "insert into %s (id, data) values (?, ?)"
                , H2TreeStore.NODE_ATTRIBUTES_BINARY_DATA_TABLE_NAME));
        byte[] oArr = new byte[]{1,2,3};
        ByteArrayInputStream data = new ByteArrayInputStream(oArr);
        st.setInt(1, attr.getId());
        st.setBinaryStream(2, data);
        st.executeUpdate();
        con.commit();

        InputStream result = store.getNodeAttributeBinaryData(attr);
        assertNotNull(result);
        byte[] rArr = IOUtils.toByteArray(result);
        assertArrayEquals(oArr, rArr);
    }

    @Test
    public void insertNodeAttributeBinaryData() throws IOException
    {
        store.removeNodes();
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);

        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setOwner(node);
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setTemplateExpression(true);
        store.saveNodeAttribute(attr);

        byte[] oArr = new byte[]{1,2,3};
        ByteArrayInputStream data = new ByteArrayInputStream(oArr);
        store.saveNodeAttributeBinaryData(attr, data);

        InputStream result = store.getNodeAttributeBinaryData(attr);
        assertNotNull(result);
        byte[] rArr = IOUtils.toByteArray(result);
        assertArrayEquals(oArr, rArr);
    }

    @Test
    public void updateNodeAttributeBinaryData() throws Exception
    {
        store.removeNodes();
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);

        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setOwner(node);
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setTemplateExpression(true);
        store.saveNodeAttribute(attr);

        byte[] oArr = new byte[]{1,2,3};
        ByteArrayInputStream data = new ByteArrayInputStream(oArr);
        store.saveNodeAttributeBinaryData(attr, data);
        oArr = new byte[]{3,2,1};
        data = new ByteArrayInputStream(oArr);
        store.saveNodeAttributeBinaryData(attr, data);

        InputStream result = store.getNodeAttributeBinaryData(attr);
        assertNotNull(result);
        byte[] rArr = IOUtils.toByteArray(result);
        assertArrayEquals(oArr, rArr);
    }

    @Test
    public void removeNodeAttributeBinaryData() throws Exception
    {
        store.removeNodes();
        ContainerNode node = new ContainerNode();
        node.setName("node");
        store.saveNode(node);

        NodeAttributeImpl attr = new NodeAttributeImpl();
        attr.setName("name");
        attr.setOwner(node);
        attr.setType(String.class);
        attr.setRequired(false);
        attr.setRawValue("value");
        attr.setTemplateExpression(true);
        store.saveNodeAttribute(attr);

        byte[] oArr = new byte[]{1,2,3};
        ByteArrayInputStream data = new ByteArrayInputStream(oArr);
        store.saveNodeAttributeBinaryData(attr, data);
        oArr = new byte[]{3,2,1};
        data = new ByteArrayInputStream(oArr);
        store.saveNodeAttributeBinaryData(attr, data);

        assertTrue(store.hasNodeAttributeBinaryData(attr));
        store.removeNodeAttributeBinaryData(attr);
        assertFalse(store.hasNodeAttributeBinaryData(attr));
    }
}
