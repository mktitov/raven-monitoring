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

import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.LeafNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.store.TreeStore;
import org.weda.constraints.ConstraintException;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateWizardTest extends ServiceTestCase
{
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void test() throws ConstraintException
    {
        //initialization block
        Tree tree = registry.getService(Tree.class);
        Configurator configurator = registry.getService(Configurator.class);
        TreeStore store = configurator.getTreeStore();
        TypeConverter converter = registry.getService(TypeConverter.class);
        
        store.removeNodes();
        tree.reloadTree();
        
        TemplatesNode templates = (TemplatesNode)tree.getRootNode().getChildren(TemplatesNode.NAME);
        TemplateNode template = new TemplateNode();
        template.setName("template");
        templates.addChildren(template);
        store.saveNode(template);
        template.init();
        template.start();
        assertEquals(Node.Status.STARTED, template.getStatus());
        TemplateVariablesNode varsNode = template.getVariablesNode();
        NodeAttribute stringVar = new NodeAttributeImpl("stringVar", String.class, null, null);
        stringVar.setOwner(varsNode);
        varsNode.addNodeAttribute(stringVar);
        store.saveNodeAttribute(stringVar);
        NodeAttribute integerVar = new NodeAttributeImpl("integerVar", Integer.class, null, null);
        integerVar.setOwner(varsNode);
        integerVar.setRequired(true);
        varsNode.addNodeAttribute(integerVar);
        store.saveNodeAttribute(integerVar);
        
        Node node = new ContainerNode("node");
        template.getEntryNode().addChildren(node);
        store.saveNode(node);
        node.init();
        NodeAttribute stringAttr =
                new NodeAttributeImpl("stringAttr", TemplateVariable.class, null, null);
        stringAttr.setOwner(node);
        node.addNodeAttribute(stringAttr);
        stringAttr.setValue(converter.convert(String.class, new TemplateVariable(stringVar), null));
        store.saveNodeAttribute(stringAttr);
        
        Node child = new LeafNode("child");
        node.addChildren(child);
        store.saveNode(child);
        child.init();
        NodeAttribute integerAttr = 
                new NodeAttributeImpl("integerAttr", TemplateVariable.class, null, null);
        integerAttr.setOwner(child);
        child.addNodeAttribute(integerAttr);
        integerAttr.setValue(
                converter.convert(String.class, new TemplateVariable(integerVar), null));
        store.saveNodeAttribute(integerAttr);
        
        //test
        TemplateWizard wizard = new TemplateWizard(template, tree.getRootNode(), "newName");
        try{
            wizard.createNodes();
            fail();
        }catch(ConstraintException e){           
        }
        wizard.getVariablesNode().getNodeAttribute("integerVar").setValue("10");
        wizard.createNodes();
        
        checkCreatedNodes(tree);
        
        tree.reloadTree();
        
        checkCreatedNodes(tree);
    }
    
    private void checkCreatedNodes(Tree tree)
    {
        Node newNode = tree.getRootNode().getChildren("newName");
        assertNotNull(newNode);
        assertEquals(Status.STARTED, newNode.getStatus());
        NodeAttribute attr = newNode.getNodeAttribute("stringAttr");
        assertNotNull(attr);
        assertNull(attr.getValue());
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
