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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ActionNodeTest extends RavenCoreTestCase
{
    ActionNode action;

    @Before
    public void prepare()
    {
        action = new ActionNode();
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
        List<ViewableObject> voList = action.getViewableObjects(null);
        assertNotNull(voList);
        assertEquals(1, voList.size());
        assertTrue(voList.get(0) instanceof ActionViewableObject);
        ActionViewableObject vo = (ActionViewableObject) voList.get(0);
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

        List<ViewableObject> voList = action.getViewableObjects(refreshAttributes);
        assertNotNull(voList);
        assertEquals(1, voList.size());
        assertEquals("hello world", voList.get(0).getData());
    }

    @Test
    public void disabledActionTest() throws Exception
    {
        action.setActionEnabled(false);
        assertTrue(action.start());
        List<ViewableObject> voList = action.getViewableObjects(null);
        assertNotNull(voList);
        assertEquals(1, voList.size());
        assertEquals("action disabled", voList.get(0).getData());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, voList.get(0).getMimeType());
        
    }
}