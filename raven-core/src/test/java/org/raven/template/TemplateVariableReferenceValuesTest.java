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

import java.util.List;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.constraints.ReferenceValue;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateVariableReferenceValuesTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception 
    {
        NodePathResolver pathResolver = registry.getService(NodePathResolver.class);
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
        NodeAttribute attr = new NodeAttributeImpl("attr", String.class, null, null);
        attr.setValueHandlerType(TemplateVariableValueHandlerFactory.TYPE);
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.init();
        store.saveNodeAttribute(attr);
        
        assertNull(tree.getReferenceValuesForAttribute(attr));
        
        NodeAttribute var = new NodeAttributeImpl("var1", String.class, "1", null);
        var.setOwner(vars);
        vars.addNodeAttribute(var);
        var.init();
        
        NodeAttribute avar = new NodeAttributeImpl("avar1", String.class, "1", null);
        avar.setOwner(vars);
        vars.addNodeAttribute(avar);
        avar.init();
        
        List<ReferenceValue> values = tree.getReferenceValuesForAttribute(attr);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertEquals(avar.getName(), values.get(0).getValueAsString());
        assertEquals(pathResolver.getAbsolutePath(avar), values.get(0).getValue());
        assertEquals(var.getName(), values.get(1).getValueAsString());
        assertEquals(pathResolver.getAbsolutePath(var), values.get(1).getValue());
    }
}
