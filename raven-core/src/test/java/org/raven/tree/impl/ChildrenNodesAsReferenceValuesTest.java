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

package org.raven.tree.impl;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValueCollection;
import static org.easymock.EasyMock.*;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class ChildrenNodesAsReferenceValuesTest extends RavenCoreTestCase
{
    @Test
    public void invalidAttributeTypeTest() throws TooManyReferenceValuesException
    {
        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getValueHandlerType()).andReturn("INVALID_TYPE");
        
        ReferenceValueCollection valuesCollection = createMock(ReferenceValueCollection.class);

        replay(attr, valuesCollection);

        ChildrenNodesAsReferenceValues refValues =
                new ChildrenNodesAsReferenceValues("TEST_TYPE", "/datasources");

        assertFalse(refValues.getReferenceValues(attr, valuesCollection));
        
        verify(attr, valuesCollection);
    }

    @Test
    public void validAttributeTypeTest() throws TooManyReferenceValuesException
    {
        ContainerNode node = new ContainerNode("datasources");
        tree.getRootNode().addAndSaveChildren(node);
        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);
        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        NodeAttribute attr = createMock(NodeAttribute.class);
        expect(attr.getValueHandlerType()).andReturn("TEST_TYPE");

        ReferenceValueCollection valuesCollection = createMock(ReferenceValueCollection.class);
        valuesCollection.add(new ReferenceValueImpl(node1.getPath(), node1.getName()), null);
        valuesCollection.add(new ReferenceValueImpl(node2.getPath(), node2.getName()), null);

        replay(attr, valuesCollection);

        ChildrenNodesAsReferenceValues refValues =
                new ChildrenNodesAsReferenceValues("TEST_TYPE", "/datasources");


        assertTrue(refValues.getReferenceValues(attr, valuesCollection));

        verify(attr, valuesCollection);
    }
}