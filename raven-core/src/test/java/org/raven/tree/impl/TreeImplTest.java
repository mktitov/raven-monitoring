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

package org.raven.tree.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.easymock.IArgumentMatcher;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.impl.NodeClassTransformerWorker;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.AttributeValueHandlerRegistry;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.TreeError;
import org.raven.tree.TreeException;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.internal.services.ResourceProvider;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.impl.ReferenceValueImpl;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class TreeImplTest extends RavenCoreTestCase
{
    @Test
    public void saveNodeTest()
    {
        BaseNode container = new BaseNode("container");
        tree.saveNode(container);
        assertTrue(container.getId()>0);
    }

    @Test
    public void saveDynamicNodeTest()
    {
        BaseNode container = new BaseNode("container");
        container.setChildrensDynamic(true);

        BaseNode child = new BaseNode("child");
        child.setParent(container);
        tree.saveNode(child);
        assertEquals(-1, child.getId());
        
        child = new BaseNode("child2");
        child.setParent(container);
        tree.saveNode(child);
        assertEquals(-2, child.getId());
    }

    @Test
    public void saveNodeAttributeTest()
    {
        BaseNode container = new BaseNode("container");
        tree.saveNode(container);
        assertTrue(container.getId()>0);

        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, "value", "desc");
        attr.setOwner(container);
        tree.saveNodeAttribute(attr);
        assertTrue(attr.getId()>0);
    }

    @Test
    public void saveDynamicNodeAttributeTest()
    {
        BaseNode container = new BaseNode("container");
        container.setChildrensDynamic(true);

        BaseNode child = new BaseNode("child");
        child.setParent(container);
        tree.saveNode(child);
        assertEquals(-1, child.getId());

        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, "value", "desc");
        attr.setOwner(child);
        tree.saveNodeAttribute(attr);
        assertEquals(-1, attr.getId());

        attr = new NodeAttributeImpl("test2", String.class, "value", "desc");
        attr.setOwner(child);
        tree.saveNodeAttribute(attr);
        assertEquals(-2, attr.getId());
    }

    @Test
    public void saveNodeAttributeBinaryDataTest() throws IOException
    {
        BaseNode container = new BaseNode("container");
        tree.saveNode(container);
        
        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, "value", "desc");
        attr.setOwner(container);
        tree.saveNodeAttribute(attr);

        byte[] data = new byte[]{1, 2, 3};
        InputStream is = new ByteArrayInputStream(data);
        tree.saveNodeAttributeBinaryData(attr, is);

        is = configurator.getTreeStore().getNodeAttributeBinaryData(attr);
        assertNotNull(is);
        byte[] res = IOUtils.toByteArray(is);
        assertArrayEquals(data, res);
    }

    @Test(expected=TreeError.class)
    public void saveNodeAttributeBinaryData_dynamicNodeTest()
    {
        BaseNode container = new BaseNode("container");
        container.setChildrensDynamic(true);

        BaseNode child = new BaseNode("child");
        child.setParent(container);

        NodeAttributeImpl attr = new NodeAttributeImpl("test", String.class, "value", "desc");
        attr.setOwner(child);

        byte[] data = new byte[]{1, 2, 3};
        InputStream is = new ByteArrayInputStream(data);
        tree.saveNodeAttributeBinaryData(attr, is);
    }

    @Test
    public void moveNodeTest() throws TreeException, InvalidPathException
    {
        Node parentNode = new BaseNode("parent");
        tree.getRootNode().addAndSaveChildren(parentNode);
        assertTrue(parentNode.start());

        Node node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        tree.move(node, parentNode, null);

        tree.reloadTree();

        Node newNode = tree.getNode("/parent/node");
        assertNotNull(newNode);
        assertEquals(node, newNode);
    }

    @Test
    public void moveAndRenameNodeTest() throws TreeException, InvalidPathException
    {
        Node parentNode = new BaseNode("parent");
        tree.getRootNode().addAndSaveChildren(parentNode);
        assertTrue(parentNode.start());

        Node node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        tree.move(node, parentNode, "newName");

        tree.reloadTree();

        Node newNode = tree.getNode("/parent/newName");
        assertNotNull(newNode);
        assertEquals(node, newNode);
    }

    @Test
    public void getReferenceValuesForAttribute() throws IOException, Exception
    {
        NodeAttribute integerAttr = 
                new NodeAttributeImpl("integerAttribute", Integer.class, null, null);
        integerAttr.setId(1);
        
        AttributeReferenceValues referenceValues = 
                createMock("AttributeReferenceValues", AttributeReferenceValues.class);
//        Configurator configurator = createMock("Configurator", Configurator.class);
//        TreeStore store = createMock("TreeStore", TreeStore.class);
        ResourceProvider resourceProvider = createMock("ResourceProvider", ResourceProvider.class);
        NodePathResolver pathResolver = createMock("NodePathResolver", NodePathResolver.class);
        AttributeValueHandlerRegistry valueHandlerRegistry = 
                createMock("AttributeValueHandlerRegistry", AttributeValueHandlerRegistry.class);
        
//        expect(configurator.getTreeStore()).andReturn(store).anyTimes();
//        ContainerNode rootNode = new ContainerNode("");
//        expect(store.getRootNode()).andReturn(rootNode);
//        store.saveNode(isA(Node.class));
//        expectLastCall().anyTimes();
        resourceProvider.getResourceStrings(NodeClassTransformerWorker.NODES_TYPES_RESOURCE);
        expectLastCall().andReturn(Collections.EMPTY_LIST);
        referenceValues.getReferenceValues(
                (NodeAttribute)notNull(), (ReferenceValueCollection)notNull());
        expectLastCall().andReturn(true);
        referenceValues.getReferenceValues(
                (NodeAttribute)notNull(), matchCollection());
        expectLastCall().andReturn(true);
                
        replay(referenceValues, resourceProvider, valueHandlerRegistry);
        
        TreeImpl tree = new TreeImpl(
                referenceValues, configurator, resourceProvider, pathResolver
                , valueHandlerRegistry);
        tree.reloadTree();
        
        assertNull(tree.getReferenceValuesForAttribute(integerAttr));
        List<ReferenceValue> values = tree.getReferenceValuesForAttribute(integerAttr);
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("value", values.get(0).getValue());
        assertEquals("valueAsString", values.get(0).getValueAsString());
//        assertSame(oneList, tree.getReferenceValuesForAttribute(numberAttr));
//        assertSame(twoList, tree.getReferenceValuesForAttribute(integerAttr));
//        assertNull(tree.getReferenceValuesForAttribute(stringAttr));
        
        verify(referenceValues, resourceProvider, valueHandlerRegistry);
    }
    
    private static ReferenceValueCollection matchCollection()
    {
        reportMatcher(new IArgumentMatcher() {

            public boolean matches(Object argument)
            {
                try
                {
                    ReferenceValueCollection values = (ReferenceValueCollection) argument;
                    values.add(new ReferenceValueImpl("value", "valueAsString"), null);
                } catch (TooManyReferenceValuesException ex)
                {
                }
                return true;
            }

            public void appendTo(StringBuffer buffer)
            {
            }
        });
        return null;
    }
}
