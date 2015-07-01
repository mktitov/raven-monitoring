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

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestUserContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.test.UserContextServiceModule;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ShowIfNodeTest extends RavenCoreTestCase
{
    private ContainerNode container;
    private ShowIfNode ifnode;
    private TextNode node;

    @Before
    public void prepare() throws Exception
    {
        container = new ContainerNode("container");
        tree.getRootNode().addAndSaveChildren(container);
        assertTrue(container.start());

        ifnode = new ShowIfNode();
        ifnode.setName("ifnode");
        container.addAndSaveChildren(ifnode);

        node = new TextNode();
        node.setName("text");
        ifnode.addAndSaveChildren(node);
        node.setText("test");
        NodeAttributeImpl attr = new NodeAttributeImpl("attr", String.class, null, null);
        attr.setValueHandlerType(RefreshAttributeValueHandlerFactory.TYPE);
        attr.setOwner(node);
        attr.init();
        node.addNodeAttribute(attr);
        assertTrue(node.start());
    }

    @Test
    public void trueExpressionTest() throws Exception
    {
        ifnode.setExpression(true);
        assertTrue(ifnode.start());
        checkChilds();
    }

    @Test
    public void falseExpressionTest() throws Exception
    {
        ifnode.setExpression(false);
        assertNull(ifnode.getRefreshAttributes());
        assertNull(ifnode.getViewableObjects(null));
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

    private void checkChilds() throws Exception
    {
        Map<String, NodeAttribute> refAttrs = ifnode.getRefreshAttributes();
        assertNotNull(refAttrs);
        assertEquals(1, refAttrs.size());
        assertTrue(refAttrs.containsKey("attr"));

        List<ViewableObject> vos = ifnode.getViewableObjects(refAttrs);
        assertNotNull(vos);
        assertEquals(1, vos.size());
        assertEquals("test", vos.get(0).getData());
    }
}