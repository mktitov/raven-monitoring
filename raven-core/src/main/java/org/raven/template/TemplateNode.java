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

import org.raven.annotations.NodeClass;
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
    public final static String VARIABLES_NODE = "Variables";
    public final static String ENTRY_NODE = "Entry";
    
    private TemplateVariablesNode variablesNode;
    private TemplateEntry entryNode;
    
    @Override
    protected void doInit()
    {
        variablesNode = (TemplateVariablesNode) getChildren(VARIABLES_NODE);
        if (variablesNode==null)
        {
            variablesNode = new TemplateVariablesNode();
            variablesNode.setName(VARIABLES_NODE);
            addChildren(variablesNode);
            configurator.getTreeStore().saveNode(variablesNode);
            variablesNode.init();
            variablesNode.start();
        }
        
        entryNode = (TemplateEntry) getChildren(ENTRY_NODE);
        if (entryNode==null)
        {
            entryNode = new TemplateEntry();
            entryNode.setName(ENTRY_NODE);
            addChildren(entryNode);
            configurator.getTreeStore().saveNode(entryNode);
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
}
