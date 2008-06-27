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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.raven.tree.AttributeReferenceValues;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.impl.AttributeNameComporator;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateVariableReferenceValues implements AttributeReferenceValues
{
    @Service
    private static NodePathResolver pathResolver;
            
    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues) 
            throws TooManyReferenceValuesException
    {
        if (!TemplateVariableValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
            return false;
        
        Node node = attr.getOwner();
        while ( (node=node.getParent())!=null )
        {
            if (node instanceof TemplateNode)
            {
                TemplateVariablesNode varsNode = ((TemplateNode)node).getVariablesNode();
                Collection<NodeAttribute> attrs = varsNode.getNodeAttributes();
                if (attrs!=null && attrs.size()>0)
                {
                    List<NodeAttribute> vars = 
                            new ArrayList<NodeAttribute>(varsNode.getNodeAttributes());
                    Collections.sort(vars, new AttributeNameComporator());
                    for (NodeAttribute var: vars)
                    {
                        ReferenceValue referenceValue = new ReferenceValueImpl(
                            pathResolver.getAbsolutePath(var), var.getName());
                        referenceValues.add(referenceValue, null);
                    }
                }
                break;
            }
        }
        return true;
    }
}
