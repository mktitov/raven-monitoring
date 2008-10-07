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

package org.raven.expr.impl;

import java.util.Collection;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.template.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class IfNodeTest extends RavenCoreTestCase
{
    @Test
    public void inTemplate() throws Exception
    {
        TemplateEntry templateEntry = new TemplateEntry();
        templateEntry.setName("templateEntry");
        tree.getRootNode().addChildren(templateEntry);
        templateEntry.init();
        
        IfNode ifNode = new IfNode();
        ifNode.setName("if");
        templateEntry.addChildren(ifNode);
        ifNode.save();
        ifNode.init();
        Node child = new BaseNode("child");
        ifNode.addChildren(child);
        child.save();
        child.init();
        
        ifNode.getNodeAttribute(IfNode.USEDINTEMPLATE_ATTRIBUTE).setValue("true");
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue("false");
        assertTrue(ifNode.isConditionalNode());
        assertNull(ifNode.getEffectiveChildrens());
        
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue("true");
        Collection<Node> childs = ifNode.getEffectiveChildrens();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(child, childs.iterator().next());
        
        ifNode.getNodeAttribute(IfNode.USEDINTEMPLATE_ATTRIBUTE).setValue("false");
        assertFalse(ifNode.isConditionalNode());
        assertNull(ifNode.getEffectiveChildrens());
    }
    
    @Test
    public void notInTemplate() throws Exception
    {
        IfNode ifNode = new IfNode();
        ifNode.setName("if");
        tree.getRootNode().addChildren(ifNode);
        ifNode.save();
        ifNode.init();
        Node child = new BaseNode("child");
        ifNode.addChildren(child);
        child.save();
        child.init();
        
        ifNode.getNodeAttribute(IfNode.USEDINTEMPLATE_ATTRIBUTE).setValue("false");
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue("false");
        assertTrue(ifNode.isConditionalNode());
        assertNull(ifNode.getEffectiveChildrens());
        
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue("true");
        Collection<Node> childs = ifNode.getEffectiveChildrens();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertSame(child, childs.iterator().next());
        
        ifNode.getNodeAttribute(IfNode.USEDINTEMPLATE_ATTRIBUTE).setValue("true");
        assertFalse(ifNode.isConditionalNode());
        assertNull(ifNode.getEffectiveChildrens());
    }
    
}
