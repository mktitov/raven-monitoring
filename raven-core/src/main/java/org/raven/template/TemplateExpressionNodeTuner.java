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
import org.raven.tree.NodeError;
import org.raven.tree.NodeTuner;
import org.weda.beans.ObjectUtils;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateExpressionNodeTuner implements NodeTuner
{
    public final static String TEMPLATE_EXPRESSION_PREFIX = "^t";
    
    @Service
    private static ExpressionCompiler expressionCompiler;
    @Service
    private static TypeConverter converter;
    
    public Node cloneNode(Node sourceNode) 
    {
        Bindings bindings = null;
        if (   sourceNode.getName().startsWith(TEMPLATE_EXPRESSION_PREFIX) 
            && (bindings=getBindings(sourceNode))!=null)
        {
            Node sourceClone = null;
            try {
                sourceClone = (Node) sourceNode.clone();
            } catch (CloneNotSupportedException ex) { 
                sourceNode.getLogger().error(
                        String.format("Error cloning (%s) node", sourceNode.getPath())
                        , ex);
            }
            String newNodeName = evalExpression(
                    sourceNode.getName().substring(TEMPLATE_EXPRESSION_PREFIX.length())
                    , bindings, sourceNode, null);
            sourceClone.setName(newNodeName);
            
            return sourceClone;
        }
        else
            return null;
    }

    public void tuneNode(Node sourceNode, Node sourceClone) 
    {
        Bindings bindings = getBindings(sourceNode);
        if (bindings==null)
            return;
        
        Collection<NodeAttribute> attrs = sourceNode.getNodeAttributes();
        if (attrs!=null)
            for (NodeAttribute attr: attrs)
                if (attr.isTemplateExpression())
                {
                    String strResult = evalExpression(attr.getRawValue(), bindings, sourceNode, attr);

                    NodeAttribute attrClone = sourceClone.getNodeAttribute(attr.getName());
                    attrClone.setTemplateExpression(false);
                    attrClone.setRawValue(strResult);
                }
    }

    public void finishTuning(Node sourceClone) { }
    
    private Bindings getBindings(Node sourceNode)
    {
        Bindings bindings = new SimpleBindings();
        sourceNode.formExpressionBindings(bindings);
        formBindings(bindings);
        //geting the template that now work
        Node templateNode = (Node) bindings.get(TemplateNode.TEMPLATE_EXPRESSION_BINDING);
        if (templateNode==null)
            return null;
        
        //testing that working template and sourceNode template is the same nodes
        if (!ObjectUtils.equals(sourceNode.getTemplate(), templateNode))
            return null;
        
        return bindings;
    }
    
    protected void formBindings(Bindings bindings) { }
    
    private String evalExpression(
            String expressionStr, Bindings bindings, Node sourceNode, NodeAttribute attr)
    {
        try 
        {
            if (expressionStr==null)
                return null;
            
            Expression expression = expressionCompiler.compile(expressionStr, "groovy");
            Object result = expression.eval(bindings);
            String strResult = converter.convert(String.class, result, null);
            return strResult;
        } 
        catch (Exception e)
        {
            String errorMessage = 
                    String.format("Error evaluating template expression (%s) ", expressionStr);
            if (attr==null)
                String.format(errorMessage+"for name of the node (%s)", sourceNode.getPath());
            else
                String.format(errorMessage+"for attribute (%s) of the node (%s)"
                        , attr.getName(), sourceNode.getPath());
            throw new NodeError(errorMessage, e);
        }
    }
}
