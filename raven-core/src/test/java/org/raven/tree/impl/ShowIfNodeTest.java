/*
 *  Copyright 2011 Mikhail Titov.
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

import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestUserContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.test.UserContextServiceModule;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class ShowIfNodeTest extends RavenCoreTestCase
{
    private ContainerNode container;
    private ShowIfNode ifnode;
    private ContainerNode node;

    @Before
    public void prepare()
    {
        container = new ContainerNode("container");
        tree.getRootNode().addAndSaveChildren(container);
        assertTrue(container.start());

        ifnode = new ShowIfNode();
        ifnode.setName("ifnode");
        container.addAndSaveChildren(ifnode);

        node = new ContainerNode("node");
        ifnode.addAndSaveChildren(node);
        assertTrue(node.start());
    }

    @Test
    public void trueExpressionTest()
    {
        ifnode.setExpression(true);
        assertTrue(ifnode.start());
        checkChilds();
    }

    @Test
    public void falseExpressionTest()
    {
        ifnode.setExpression(false);
        assertTrue(ifnode.start());
        assertNull(container.getEffectiveChildrens());
    }

    @Test
    public void scriptTest() throws Exception
    {
        NodeAttribute attr = ifnode.getNodeAttribute(ShowIfNode.EXPRESSION_ATTR);
        attr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        attr.setValue("userContext.params.true");
        assertTrue(ifnode.start());

        TestUserContext userContext = new TestUserContext();
        userContext.getParams().put("true", true);
        UserContextServiceModule.setUserContext(userContext);
        checkChilds();
    }

    private void checkChilds()
    {
        Collection<Node> childs = container.getEffectiveChildrens();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(node, childs.iterator().next());
    }
}