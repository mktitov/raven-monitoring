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
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeTuner;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateExpressionNodeTuner implements NodeTuner
{
    @Service
    private static ExpressionCompiler expressionCompiler;
    @Service
    private static TypeConverter converter;
    
    public Node cloneNode(Node sourceNode) 
    {
        return null;
    }

    public void tuneNode(Node sourceNode, Node sourceClone) 
    {
        Bindings bindings = new SimpleBindings();
        sourceNode.formExpressionBindings(bindings);
        //geting the template that now work
        Node templateNode = (Node) bindings.get(TemplateNode.TEMPLATE_EXPRESSION_BINDING);
        if (templateNode==null)
            return;
        
        //testing that working template and sourceNode template is the same nodes
        if (!ObjectUtils.equals(sourceNode.getTemplate(), templateNode))
            return;
        
        Collection<NodeAttribute> attrs = sourceNode.getNodeAttributes();
        if (attrs!=null)
            for (NodeAttribute attr: attrs)
                if (attr.isTemplateExpression())
                {
                    try {
                        Expression expression = 
                                expressionCompiler.compile(attr.getValue(), "groovy");
                        Object result = expression.eval(bindings);
                        String strResult = converter.convert(String.class, result, null);
                        
                        NodeAttribute attrClone = sourceClone.getNodeAttribute(attr.getName());
                        attrClone.setTemplateExpression(false);
                        attrClone.setRawValue(strResult);
                    } 
                    catch (ScriptException ex) {
                        sourceNode.getLogger().error(
                            String.format(
                                "Error compiling template expression (%s) for attribute (%s) " +
                                "of node (%s)"
                                , attr.getValue(), attr.getName(), sourceNode.getPath())
                            , ex);
                    }
                }
                
    }

    public void finishTuning(Node sourceClone) { }
}
