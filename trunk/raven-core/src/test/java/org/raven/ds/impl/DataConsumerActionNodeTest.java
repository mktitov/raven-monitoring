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

package org.raven.ds.impl;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.ActionNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class DataConsumerActionNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        NodeAttributeImpl consAttr = 
                new NodeAttributeImpl("dsAttr", String.class, "dsAttrValue", null);
        ds.addConsumerAttribute(consAttr);

        DataConsumerActionNode action = new DataConsumerActionNode();
        action.setName("action");
        tree.getRootNode().addAndSaveChildren(action);
        action.setDataSource(ds);
        setExpression(action, ActionNode.ACTION_ENABLED_ATTR, "data.size()==1");
        setExpression(action, ActionNode.CONFIRMATION_MESSAGE_ATTR, "''+data.size()+'?'");
        setExpression(action, ActionNode.ENABLED_ACTION_TEXT_ATTR, "data.size()");
        setExpression(action, ActionNode.ACTION_EXPRESSION_ATTR, "data[0]");
        assertTrue(action.start());

        ds.addDataPortion("test");
        List<ViewableObject> objects = action.getViewableObjects(null);
        assertNotNull(objects);
        assertEquals(1, objects.size());
        assertTrue(objects.get(0) instanceof ActionViewableObject);
        ActionViewableObject actionVO = (ActionViewableObject) objects.get(0);
        assertEquals("1", actionVO.toString());
        assertEquals("1?", actionVO.getConfirmationMessage());
        assertEquals("test", actionVO.getData());

        Map<String, NodeAttribute> sessAttrs = ds.getLastSessionAttributes();
        assertNotNull(sessAttrs);
        assertEquals(1, sessAttrs.size());
        assertTrue(sessAttrs.containsKey("dsAttr"));
    }

    private void setExpression(DataConsumerActionNode action, String attrName, String expression)
            throws Exception
    {
        NodeAttribute attr = action.getNodeAttribute(attrName);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.setValue(expression);
    }
}