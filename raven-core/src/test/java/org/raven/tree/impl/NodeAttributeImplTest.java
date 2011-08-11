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

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.objects.NodeWithReadonlyParameter;
import org.raven.ds.ReferenceValuesSource;
import org.raven.ds.ValueValidatorController;
import org.raven.tree.AttributeValueValidationException;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeImplTest extends RavenCoreTestCase
{
    @Test
    public void readOnlyAttributeTest()
    {
        NodeWithReadonlyParameter node = new NodeWithReadonlyParameter();
        node.setName("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();

        assertEquals(NodeWithReadonlyParameter.VALUE, node.getReadOnlyParameter());
        NodeAttribute attr = node.getNodeAttribute("readOnlyParameter");
        assertNotNull(attr);
        assertEquals(NodeWithReadonlyParameter.VALUE, attr.getValue());
    }

    @Test
    public void referenceValuesSourceTest() throws Exception
    {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());
        
        NodeAttributeImpl attr = new NodeAttributeImpl("name", String.class, null, null);
        attr.setOwner(node);
        attr.init();
        node.addNodeAttribute(attr);

        assertNull(attr.getReferenceValues());

        ReferenceValuesSource source = createMock(ReferenceValuesSource.class);
        expect(source.getReferenceValues()).andReturn(Collections.EMPTY_LIST);
        replay(source);
        
        attr.setReferenceValuesSource(source);
        assertSame(Collections.EMPTY_LIST, attr.getReferenceValues());

        verify(source);
    }

    @Test
    public void valueValidatorTest() throws Exception
    {
        BaseNode node = new BaseNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        NodeAttributeImpl attr = new NodeAttributeImpl("name", String.class, null, null);
        attr.setOwner(node);
        attr.init();
        node.addNodeAttribute(attr);

        assertNull(attr.getValueValidatorController());

        ValueValidatorController validator = createStrictMock(ValueValidatorController.class);
        expect(validator.validate("test")).andReturn(null);
        expect(validator.validate("test2")).andReturn(Arrays.asList("error"));
        replay(validator);
        
        attr.setValueValidatorController(validator);
        assertSame(validator, attr.getValueValidatorController());

        attr.setValue("test");
        assertEquals("test", attr.getValue());

        try{
            attr.setValue("test2");
            fail();
        }catch(AttributeValueValidationException e){
            assertArrayEquals(new Object[]{"error"}, e.getErrors().toArray());
            assertArrayEquals(new Object[]{"error"}, attr.getValidationErrors().toArray());
            assertEquals("test", attr.getValue());
        }

        verify(validator);
    }
}
