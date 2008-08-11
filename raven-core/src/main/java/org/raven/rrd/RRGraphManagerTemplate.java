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

package org.raven.rrd;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.annotations.NodeClass;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.expr.impl.IfNode;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.template.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=RRGraphManager.class, childNodes={IfNode.class, GroupNode.class, RRGraphNode.class})
public class RRGraphManagerTemplate extends TemplateEntry
{
    public final static String NAME = "Template";
    
    public final static String AUTOCOLOR_ATTRIBUTE = "autoColor";

    public RRGraphManagerTemplate()
    {
        setName(NAME);
        setSubtreeListener(true);
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus) 
    {
        if (!(node instanceof RRGraphNode) || newStatus!=Status.INITIALIZED)
            return;
        
        addAutoColorAttribute(node);
        
        NodeAttribute exprAttr = node.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
        if (exprAttr==null)
        {
            try {
                exprAttr = new NodeAttributeImpl(
                        GroupNode.GROUPINGEXPRESSION_ATTRIBUTE, String.class, null, null);
                exprAttr.setRequired(true);
                exprAttr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
                node.addNodeAttribute(exprAttr);
                exprAttr.setOwner(node);
                exprAttr.init();

            } catch (Exception ex) {
                throw new NodeError(
                        String.format(
                            "Error adding (%s) attribute to the node (%s)"
                            , GroupNode.GROUPINGEXPRESSION_ATTRIBUTE, node.getPath())
                        , ex);
            }
        }
        
    }

    NodeAttribute addAutoColorAttribute(Node node) throws NodeError 
    {
        NodeAttribute colorAttr = new NodeAttributeImpl(
                AUTOCOLOR_ATTRIBUTE, RRColor.class, RRColor.BLACK, null);
        node.addNodeAttribute(colorAttr);
        colorAttr.setOwner(node);
        try {
            colorAttr.init();
            colorAttr.save();
        } catch (Exception ex) {
            throw new NodeError(String.format(
                    "Error adding (%s) attribute to the (%s) node"
                    , AUTOCOLOR_ATTRIBUTE, node.getPath()), ex);
        }
        
        return colorAttr;
    }
    
    
}
