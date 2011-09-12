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

package org.raven.template.impl;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.Tree;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.LeafNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.constraints.ConstraintException;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateWizardTest extends RavenCoreTestCase
{
    private TemplateNode template;
    private NodePathResolver pathResolver;

    @Before
    public void prepare() throws Exception
    {
        pathResolver = registry.getService(NodePathResolver.class);

        TemplatesNode templates = (TemplatesNode)tree.getRootNode().getChildren(TemplatesNode.NAME);
        template = new TemplateNode();
        template.setName("template");
        templates.addChildren(template);
        tree.saveNode(template);
        template.init();
        template.start();
        assertEquals(Node.Status.STARTED, template.getStatus());

        //Creating template variables
        TemplateVariablesNode varsNode = template.getVariablesNode();

        NodeAttribute stringVar = new NodeAttributeImpl("stringVar", String.class, null, null);
        stringVar.setOwner(varsNode);
        varsNode.addNodeAttribute(stringVar);
        stringVar.init();
        stringVar.save();

        NodeAttribute integerVar = new NodeAttributeImpl("integerVar", Integer.class, null, null);
        integerVar.setOwner(varsNode);
        integerVar.setRequired(true);
        varsNode.addNodeAttribute(integerVar);
        integerVar.init();
        integerVar.save();

        //Creating template entry
        Node node = new ContainerNode("node");
        template.getEntryNode().addChildren(node);
        tree.saveNode(node);
        node.init();

        NodeAttribute stringAttr =
                new NodeAttributeImpl("stringAttr", String.class, null, null);
        stringAttr.setOwner(node);
        node.addNodeAttribute(stringAttr);
        stringAttr.setValueHandlerType(TemplateVariableValueHandlerFactory.TYPE);
        stringAttr.setValue(pathResolver.getAbsolutePath(stringVar));
        stringAttr.init();
        stringAttr.save();

        Node child = new LeafNode("^t vars['stringVar']");
        node.addChildren(child);
        tree.saveNode(child);
        child.init();
        NodeAttribute integerAttr =
                new NodeAttributeImpl("integerAttr", Integer.class, null, null);
        integerAttr.setOwner(child);
        child.addNodeAttribute(integerAttr);
        integerAttr.setValueHandlerType(TemplateVariableValueHandlerFactory.TYPE);
        integerAttr.setValue(pathResolver.getAbsolutePath(integerVar));
        integerAttr.init();
        integerAttr.save();
    }

    @Test
    public void test() throws Exception
    {        
        //test
        TemplateWizard wizard = new TemplateWizard(template, tree.getRootNode(), "newName");
        try{
            wizard.createNodes();
            fail();
        }catch(ConstraintException e){           
        }
        wizard.getVariablesNode().getNodeAttribute("integerVar").setValue("10");
        wizard.getVariablesNode().getNodeAttribute("stringVar").setValue("child");
        wizard.createNodes();
        
        checkCreatedNodes(tree);
        
        tree.reloadTree();
        
        checkCreatedNodes(tree);
    }

    @Test
    public void testConstructorWithVarValues() throws Exception
    {
        Map<String, String> vals = new HashMap<String, String>();
        vals.put("integerVar", "10");
        vals.put("stringVar", "child");

        new TemplateWizard(template, tree.getRootNode(), "newName", vals).createNodes();

        checkCreatedNodes(tree);
        tree.reloadTree();
        checkCreatedNodes(tree);
    }
    
    private void checkCreatedNodes(Tree tree)
    {
        assertNull(tree.getRootNode().getChildren(TemplateWizard.TEMPLATE_VARIABLES_NODE));
        Node newNode = tree.getRootNode().getChildren("newName");
        assertNotNull(newNode);
        assertEquals(Status.STARTED, newNode.getStatus());
        NodeAttribute attr = newNode.getNodeAttribute("stringAttr");
        assertNotNull(attr);
        assertEquals("child", attr.getValue());
        assertEquals(String.class, attr.getType());
        
        Node newChild = newNode.getChildren("child");
        assertNotNull(newChild);
        assertEquals(Status.STARTED, newChild.getStatus());
        attr = newChild.getNodeAttribute("integerAttr");
        assertNotNull(attr);
        assertEquals(Integer.class, attr.getType());
        assertEquals("10", attr.getValue());
    }
}
