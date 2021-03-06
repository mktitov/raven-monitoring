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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeTuner;
import org.raven.tree.Tree;
import org.weda.constraints.ConstraintException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 * Creates nodes from tamplate
 * @author Mikhail Titov
 */
public class TemplateWizard 
{
    public static final String TEMPLATE_VARIABLES_NODE = "~TemplateVariables";
    
    @Service
    private static Tree tree;

    @Service
    private static TypeConverter converter;
    
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
                template.getVariablesNode(), destination,TEMPLATE_VARIABLES_NODE
                , null, false, false, false);
    }

    public TemplateWizard(TemplateNode templateNode, Node destination, String newNodeName
            , Map<String, String> vars)
        throws Exception
    {
        this(templateNode, destination, newNodeName);
        if (vars!=null && !vars.isEmpty())
            for (Map.Entry<String, String> entry: vars.entrySet())
                try {
                    NodeAttribute attr = variablesNode.getAttr(entry.getKey());
                    if (attr==null){
                        if (templateNode.isLogLevelEnabled(LogLevel.WARN))
                            templateNode.getLogger().warn("Template variable ({}) not found", entry.getKey());
                    } else {
                        String val = converter.convert(String.class, entry.getValue(), null);
                        attr.setValue(val);
                    }
                } catch (Exception e){
                    if (templateNode.isLogLevelEnabled(LogLevel.WARN))
                        templateNode.getLogger().warn(
                                "Error creating template. Can't set value for template variable ({})", e);
                    cancelWizard();
                    throw e;
                }
    }

    public TemplateVariablesNode getVariablesNode()
    {
        return variablesNode;
    }
    
    public List<Node> createNodes() throws ConstraintException {
        try {
            Collection<NodeAttribute> vars = variablesNode.getAttrs();
            if (vars!=null)
                for (NodeAttribute var: vars)
                    if (var.isRequired() && var.getValue()==null)
                        throw new ConstraintException(String.format(
                                "The value for required variable (%s) was not seted.", var.getName()));

            Collection<Node> nodesToCopy = template.getEntryNode().getNodes();
            List<Node> newNodes = nodesToCopy.isEmpty()? Collections.EMPTY_LIST : new ArrayList<Node>(nodesToCopy.size());
            if (!nodesToCopy.isEmpty()) {
                NodeTuner nodeTuner = new Tuner();
                boolean useNewNodeName = nodesToCopy.size()==1;
                for (Node node: nodesToCopy) {
                    Node newNode = tree.copy(
                            node, destination, useNewNodeName? newNodeName : null
                            , nodeTuner, true, false, false);
                    tree.start(newNode, false);
                    newNodes.add(newNode);
                }
            }
            try {
                template.getBindingSupport().put(TemplateNode.NEW_NODES_BINDING, newNodes);
                template.getBindingSupport().put(TemplateNode.TEMPLATE_VARIABLES_EXPRESSION_BINDING, getVars());
                template.getExecuteAfter();
            } finally {
                template.getBindingSupport().reset();
            }
            return newNodes;
        } finally {
            destination.removeChildren(variablesNode);
        }
    }
    
    public void cancelWizard() {
        destination.removeChildren(variablesNode);        
    }
    
    private Map<String, Object> getVars() {
        Collection<NodeAttribute> attrs = variablesNode.getAttrs();
        if (attrs.isEmpty()) return Collections.EMPTY_MAP;
        else {
            Map<String, Object> vars = new HashMap<String, Object>();
            for (NodeAttribute attr: attrs)
                vars.put(attr.getName(), attr.getRealValue());
            return vars;
        }
    }
    
    private class Tuner extends TemplateExpressionNodeTuner
    {
        @Override
        public void tuneNode(Node sourceNode, Node sourceClone)
        {
            super.tuneNode(sourceNode, sourceClone);
            
            Collection<NodeAttribute> attrs = sourceClone.getAttrs();
            if (attrs!=null)
                for (NodeAttribute attr: attrs)
                    if (TemplateVariableValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
                    {
                        try
                        {
                            int attrSepPos = 
                                    attr.getRawValue().lastIndexOf(Node.ATTRIBUTE_SEPARATOR);
                            String varName = attr.getRawValue().substring(++attrSepPos);
                            NodeAttribute var = variablesNode.getAttr(varName);
                            attr.setValueHandlerType(var.getValueHandlerType());
                            attr.setValue(var.getRawValue());
                        } catch (Exception ex)
                        {
                            sourceClone.getLogger().error(
                                    String.format("Error tuning node (%s)"
                                    , sourceClone.getPath()), ex);
                        }
                    }
        }

        @Override
        public Node cloneNode(Node sourceNode) {
            return super.cloneNode(sourceNode);
        }

        @Override
        public void finishTuning(Node sourceClone) { }

        @Override
        protected void formBindings(Bindings bindings) {
            bindings.put(TemplateNode.TEMPLATE_VARIABLES_EXPRESSION_BINDING, getVars());
        }
    }
}
