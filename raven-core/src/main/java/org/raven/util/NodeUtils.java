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

package org.raven.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.api.impl.NodeAccessImpl;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.template.impl.TemplateExpression;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ActionAttributeValueHandlerFactory;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeUtils
{
    @Service
    private static TypeConverter converter;

    /**
     * Returns the sorted list of the <b>started</b> child nodes of the type <b>childType</b> of the <b>owner</b> node.
     * Method returns <b>empty list</b> if owner node does not have started child of the specified type.
     * @param owner the owner node
     * @param childType the type of child node
     * @return
     */
    public static <T> List<T> getChildsOfType(Node owner, Class<T> childType)
    {

        Collection<Node> childs = owner.getSortedChildrens();
        if (childs!=null && !childs.isEmpty()){
            List<T> res = new ArrayList<T>(childs.size());
            for (Node child: childs)
                if (   Node.Status.STARTED.equals(child.getStatus())
                    && childType.isAssignableFrom(child.getClass()))
                {
                    res.add((T)child);
                }
            return res;
        }

        return Collections.EMPTY_LIST;
    }
    
    /**
     * Returns the map of cloned attributes which value handler type is
     * {@link RefreshAttributeValueHandlerFactory#TYPE}.
     * The ids of attributes are numbers less than zero.
     */
    public static Map<String, NodeAttribute> extractRefereshAttributes(Node node) throws Exception
    {
        return extractAttributes(node, RefreshAttributeValueHandlerFactory.TYPE);
    }

    /**
     * Returns the map of cloned attributes which value handler type is
     * {@link ActionAttributeValueHandlerFactory#TYPE}.
     * The ids of attributes are numbers less than zero.
     */
    public static Map<String, NodeAttribute> extractActionAttributes(Node node) throws Exception
    {
        return extractAttributes(node, ActionAttributeValueHandlerFactory.TYPE);
    }

    /**
     * Returns the map of cloned attributes which value handler type is the value of the <b>valueHandlerType</b>
     * parameter. The ids of attributes are numbers less than zero.
     * @param node the source node
     * @param valueHandlerType the value handler type
     */
    public static Map<String, NodeAttribute> extractAttributes(Node node, String valueHandlerType) throws Exception
    {
        Map<String, NodeAttribute> refreshAttributes = new LinkedHashMap<String, NodeAttribute>();
        Collection<NodeAttribute> attrs = node.getNodeAttributes();
        int id=-1;
        if (attrs!=null)
            for (NodeAttribute attr: attrs)
                if (valueHandlerType.equals(attr.getValueHandlerType()))
                {
                    NodeAttribute clone = (NodeAttribute) attr.clone();
                    clone.setOwner(node);
                    clone.setId(id--);
                    Bindings bindings = new SimpleBindings();
                    bindings.put(
                            ExpressionAttributeValueHandler.NODE_BINDING, new NodeAccessImpl(node));
                    Object val = TemplateExpression.eval(clone.getRawValue(), bindings);
                    clone.setRawValue(converter.convert(String.class, val, null));
                    clone.init();
                    refreshAttributes.put(clone.getName(), clone);
                }
        return refreshAttributes.isEmpty()? null : refreshAttributes;
    }
}
