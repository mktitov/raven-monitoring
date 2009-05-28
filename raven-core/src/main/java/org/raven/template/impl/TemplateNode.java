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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=TemplatesNode.class)
@Description("The template node")
public class TemplateNode extends BaseNode 
{
    public static final String TEMPLATE_VARIABLES_EXPRESSION_BINDING = "vars";
    public static final String VARIABLES_EXPRESSION_BINDINGS = "vars";
    public final static String VARIABLES_NODE = "Variables";
    public final static String ENTRY_NODE = "Entry";
    public final static String TEMPLATE_EXPRESSION_BINDING = "template";
    
    private TemplateVariablesNode variablesNode;
    private TemplateEntry entryNode;

    @Override
    protected void initFields() 
    {
        super.initFields();
        
        variablesNode = null;
        entryNode = null;
    }

    @Override
    protected void doInit()
    {
        variablesNode = (TemplateVariablesNode) getChildren(VARIABLES_NODE);
        if (variablesNode==null)
        {
            variablesNode = new TemplateVariablesNode();
            variablesNode.setName(VARIABLES_NODE);
            addChildren(variablesNode);
            tree.saveNode(variablesNode);
            variablesNode.init();
            variablesNode.start();
        }
        
        entryNode = (TemplateEntry) getChildren(ENTRY_NODE);
        if (entryNode==null)
        {
            entryNode = new TemplateEntry();
            entryNode.setName(ENTRY_NODE);
            addChildren(entryNode);
            tree.saveNode(entryNode);
            entryNode.init();   
            entryNode.start();
        }
    }
    
    public TemplateVariablesNode getVariablesNode()
    {
        return variablesNode;
    }
    
    public TemplateEntry getEntryNode()
    {
        return entryNode;
    }

    @Override
    public void formExpressionBindings(Bindings bindings) 
    {
        super.formExpressionBindings(bindings);
        bindings.put(TEMPLATE_EXPRESSION_BINDING, this);
        Collection<NodeAttribute> varsAttrs = variablesNode.getNodeAttributes();
        Map<String, Object> vars = Collections.EMPTY_MAP;
        if (varsAttrs!=null)
        {
            vars = new HashMap<String, Object>();
            for (NodeAttribute var: varsAttrs)
                vars.put(var.getName(), var.getRealValue());
        }
        bindings.put(VARIABLES_EXPRESSION_BINDINGS, vars);
    }
    
}
