/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.impl.DataContextImpl;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.ActionViewableObject;
import static org.junit.Assert.*;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractActionNodeTest extends RavenCoreTestCase
{
    AbstractActionNode action;

    @Before
    public void prepare()
    {
        action = new TestActionNode();
        action.setName("action");
        tree.getRootNode().addAndSaveChildren(action);
        action.setEnabledActionText("test action");
        action.setDisabledActionText("action disabled");
        action.setConfirmationMessage("confirmation");
    }

    @Test
    public void test() throws Exception
    {
        action.setActionExpression("1+1");
        assertTrue(action.start());
        ViewableObject obj = action.getActionViewableObject(new DataContextImpl(), null);
        assertTrue(obj instanceof ActionViewableObject);
        ActionViewableObject vo = (ActionViewableObject) obj;
        assertEquals("confirmation", vo.getConfirmationMessage());
        assertEquals("test action", vo.toString());
        assertEquals(Viewable.RAVEN_ACTION_MIMETYPE, vo.getMimeType());
        assertEquals("2", vo.getData());
    }

    @Test
    public void withRefereshAttributesTest() throws Exception
    {
        NodeAttributeImpl attr = new NodeAttributeImpl("attr", String.class, "hello ", null);
        attr.init();
        Map<String, NodeAttribute> refreshAttributes = new HashMap<String, NodeAttribute>();
        refreshAttributes.put(attr.getName(), attr);
        action.setActionExpression("refreshAttributes['attr'].value+'world'");
        assertTrue(action.start());

        ViewableObject vo = action.getActionViewableObject(new DataContextImpl(refreshAttributes), null);
        assertNotNull(vo);
        assertEquals("hello world", vo.getData());
    }

    @Test
    public void withAddiotionalBindingsTest() throws Exception
    {
        Map<String, Object> additionalBindings = new HashMap<String, Object>();
        additionalBindings.put("greeting", "hello ");
        action.setActionExpression("greeting+'world'");
        assertTrue(action.start());

        ViewableObject vo = action.getActionViewableObject(new DataContextImpl(), additionalBindings);
        assertNotNull(vo);
        assertEquals("hello world", vo.getData());
    }

    @Test
    public void disabledActionTest() throws Exception
    {
        action.setActionEnabled(false);
        assertTrue(action.start());
        ViewableObject vo = action.getActionViewableObject(new DataContextImpl(), null);
        assertNotNull(vo);
        assertEquals("action disabled", vo.getData());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vo.getMimeType());
    }
}