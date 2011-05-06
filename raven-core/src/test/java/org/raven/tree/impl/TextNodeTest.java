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
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.raven.TestUserContext;
import org.raven.auth.UserContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.RavenCoreTestCase;
import org.raven.test.UserContextServiceModule;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class TextNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        UserContext context = new TestUserContext();
        context.getParams().put("text", " message");
        UserContextServiceModule.setUserContext(context);
        
        TextNode text = new TextNode();
        text.setName("text");
        tree.getRootNode().addAndSaveChildren(text);
        text.getNodeAttribute(TextNode.TEXT_ATTR).setValueHandlerType(
                ScriptAttributeValueHandlerFactory.TYPE);
        text.setText("refreshAttributes.text.value+userContext.params.text");
        assertTrue(text.start());

        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        NodeAttribute attr = new NodeAttributeImpl("text", String.class, "test", null);
        attr.setOwner(text);
        attr.init();
        attrs.put("text", attr);

        List<ViewableObject> vos = text.getViewableObjects(attrs);
        assertNotNull(vos);
        assertEquals(1, vos.size());
        assertEquals("test message", vos.get(0).getData());
    }
}