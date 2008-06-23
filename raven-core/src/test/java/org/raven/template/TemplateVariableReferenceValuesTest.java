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

package org.raven.template;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateVariableReferenceValuesTest extends RavenCoreTestCase
{
    @Test
    public void test() 
    {
        store.removeNodes();
        
        TemplateNode template = new TemplateNode();
        template.setName("template");
        tree.getRootNode().addChildren(template);
        store.saveNode(template);
        template.init();
        TemplateEntry entry = template.getEntryNode();
        TemplateVariablesNode vars = template.getVariablesNode();
        
        ContainerNode node = new ContainerNode("node");
        entry.addChildren(node);
        store.saveNode(node);
        node.init();
        NodeAttribute attr = new NodeAttributeImpl("attr", TemplateVariable.class, null, null);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        store.saveNodeAttribute(attr);
        
        assertEquals(Collections.EMPTY_LIST, attr.getReferenceValues());
        
        NodeAttribute var = new NodeAttributeImpl("var1", String.class, "1", null);
        var.setOwner(vars);
        vars.addNodeAttribute(var);
        
        assertEquals(
                Arrays.asList(vars.getPath()+Node.ATTRIBUTE_SEPARATOR+"var1")
                , attr.getReferenceValues());
    }
}
