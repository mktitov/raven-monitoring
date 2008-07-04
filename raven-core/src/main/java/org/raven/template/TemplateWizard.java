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

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeTuner;
import org.raven.tree.Tree;
import org.weda.internal.annotations.Service;
import org.weda.constraints.ConstraintException;

/**
 * Creates nodes from tamplate
 * @author Mikhail Titov
 */
public class TemplateWizard 
{
    @Service
    private static Tree tree;
    
    private final TemplateNode template;
    private final Node destination;
    private final String newNodeName;
    private final TemplateVariablesNode variablesNode;

    /**
     * Creates template wizard
     * @param template the template from which new node(s) will be created.
     * @param destination the destination node
     * @param newNodeName if the {@link TemplateNode template} consists of one node 
     *      ({@link TemplateEntry entry} node holds only one child node) then this name will be
     *      seted to the new node.
     */
    public TemplateWizard(TemplateNode template, Node destination, String newNodeName)
    {
        this.template = template;
        this.destination = destination;
        this.newNodeName = newNodeName;
        
        variablesNode = (TemplateVariablesNode) tree.copy(
                template.getVariablesNode(), destination, "~TemplateVariables", null, false, false);
    }

    public TemplateVariablesNode getVariablesNode()
    {
        return variablesNode;
    }
    
    public void createNodes() throws ConstraintException
    {
        Collection<NodeAttribute> vars = variablesNode.getNodeAttributes();
        if (vars!=null)
            for (NodeAttribute var: vars)
                if (var.isRequired() && var.getValue()==null)
                    throw new ConstraintException(String.format(
                            "The value for required variable (%s) was not seted.", var.getName()));
        
        Collection<Node> nodesToCopy = template.getEntryNode().getChildrens();
        if (nodesToCopy!=null)
        {
            NodeTuner nodeTuner = new Tuner();
            boolean useNewNodeName = nodesToCopy.size()==1;
            for (Node node: nodesToCopy)
            {
                Node newNode = tree.copy(
                        node, destination, useNewNodeName? newNodeName : null
                        , nodeTuner, true, true);
                tree.start(newNode);
            }
        }
        destination.removeChildren(variablesNode);
    }
    
    public void cancelWizard()
    {
        destination.removeChildren(variablesNode);        
    }
    
    private class Tuner implements NodeTuner
    {
        public void tuneNode(Node node)
        {
            Collection<NodeAttribute> attrs = node.getNodeAttributes();
            if (attrs!=null)
                for (NodeAttribute attr: attrs)
                    if (TemplateVariableValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
                    {
                        try
                        {
                            int attrSepPos = 
                                    attr.getRawValue().lastIndexOf(Node.ATTRIBUTE_SEPARATOR);
                            String varName = attr.getRawValue().substring(++attrSepPos);
                            NodeAttribute var = variablesNode.getNodeAttribute(varName);
                            attr.setValueHandlerType(var.getValueHandlerType());
                            attr.setValue(var.getRawValue());
                        } catch (Exception ex)
                        {
                            node.getLogger().error(
                                    String.format("Error tuning node (%s)", node.getPath()), ex);
                        }
                    }
//                    if (TemplateVariable.class.isAssignableFrom(attr.getType()))
//                    {
//                        String[] elems = attr.getRawValue().split(""+Node.ATTRIBUTE_SEPARATOR);
//                        NodeAttribute var = variablesNode.getNodeAttribute(elems[1]);
//                        attr.setType(var.getType());
//                        attr.setRawValue(var.getRawValue());
//                    }
        }
    }
}
