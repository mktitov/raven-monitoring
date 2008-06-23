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
import org.raven.tree.impl.AttributeReferenceImpl;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateVariableReferenceValues implements AttributeReferenceValues
{
    @Service
    private static TypeConverter converter;
    
    public List<String> getReferenceValues(NodeAttribute attr)
    {
        Node node = attr.getOwner();
        List<String> refValues = Collections.EMPTY_LIST;
        while ( (node=node.getParent())!=null )
        {
            if (node instanceof TemplateNode)
            {
                TemplateVariablesNode varsNode = ((TemplateNode)node).getVariablesNode();
                Collection<NodeAttribute> attrs = varsNode.getNodeAttributes();
                if (attrs!=null && attrs.size()>0)
                {
                    refValues = new ArrayList<String>(attrs.size());
                    for (NodeAttribute var: attrs)
                        refValues.add(converter.convert(
                            String.class, new AttributeReferenceImpl(var), null));
                    Collections.sort(refValues);
                }
                break;
            }
        }
        return refValues;
    }

    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
    {
        
        Node node = attr.getOwner();
        List<String> refValues = Collections.EMPTY_LIST;
        while ( (node=node.getParent())!=null )
        {
            if (node instanceof TemplateNode)
            {
                TemplateVariablesNode varsNode = ((TemplateNode)node).getVariablesNode();
                Collection<NodeAttribute> attrs = varsNode.getNodeAttributes();
                if (attrs!=null && attrs.size()>0)
                {
                    refValues = new ArrayList<String>(attrs.size());
                    for (NodeAttribute var: attrs)
                        refValues.add(converter.convert(
                            String.class, new AttributeReferenceImpl(var), null));
                    Collections.sort(refValues);
                }
                break;
            }
        }
        return true;
    }
}
