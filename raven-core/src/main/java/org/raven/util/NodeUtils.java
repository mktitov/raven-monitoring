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
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.log.LogLevel;
import org.raven.template.impl.TemplateExpression;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ActionAttributeValueHandlerFactory;
import org.raven.tree.impl.HiddenRefreshAttributeValueHandlerFactory;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
import org.weda.beans.ObjectUtils;
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
     */
    public static <T> List<T> getChildsOfType(Node owner, Class<T> childType)
    {
        return extractNodesOfType(owner.getSortedChildrens(), childType, true);
    }

    /**
     * Returns the sorted list of the <b>started</b> child nodes of the type <b>childType</b> of the <b>owner</b> node.
     * Method returns <b>empty list</b> if owner node does not have started child of the specified type.
     * @param owner the owner node
     * @param childType the type of child node
     * @param startedOnly if <b>true</b> then only started nodes fall into the result list
     */
    public static <T> List<T> getChildsOfType(Node owner, Class<T> childType, boolean startedOnly)
    {
        return extractNodesOfType(owner.getSortedChildrens(), childType, startedOnly);
    }

    /**
     * Returns the map of the <b>started</b> child nodes of the type <b>childType</b> of the
     * <b>owner</b> node.
     * The <b>key</b> of the returned map is the <b>name</b> of the node and the 
     * <b>value</b> is the <b>node</b>.
     * Method returns <b>empty map</b> if owner node does not have started child of the specified type.
     * @param owner the owner node
     * @param childType the type of child node
     */
    public static <T> Map<String, T> getChildsOfTypeMap(Node owner, Class<T> childType)
    {
        return getChildsOfTypeMap(owner, childType, true);
    }
    
    /**
     * Returns the map of the <b>started</b> child nodes of the type <b>childType</b> of the
     * <b>owner</b> node.
     * The <b>key</b> of the returned map is the <b>name</b> of the node and the
     * <b>value</b> is the <b>node</b>.
     * Method returns <b>empty map</b> if owner node does not have started child of the specified type.
     * @param owner the owner node
     * @param childType the type of child node
     * @param startedOnly if <b>true</b> then only started nodes fall into the result map
     */
    public static <T> Map<String, T> getChildsOfTypeMap(Node owner, Class<T> childType, boolean startedOnly)
    {
        Collection<Node> childs = owner.getSortedChildrens();
        if (childs!=null && !childs.isEmpty()){
            Map<String, T> res = new HashMap<String, T>();
            for (Node child: childs)
                if (   (Node.Status.STARTED.equals(child.getStatus()) || !startedOnly)
                    && childType.isAssignableFrom(child.getClass()))
                {
                    res.put(child.getName(), (T)child);
                }
            return res;
        }

        return Collections.EMPTY_MAP;
    }



    /**
     * Returns the sorted list of the <i>STARTED</i> effective child nodes of the type
     * <b>childType</b> of the <b>owner</b> node.
     * Method returns <b>empty list</b> if owner node does not have started child of the specified type.
     * @param owner the owner node
     * @param childType the type of child node
     */
    public static <T> List<T> getEffectiveChildsOfType(Node owner, Class<T> childType)
    {
        return extractNodesOfType(owner.getEffectiveNodes(), childType);
    }

    /**
     * Extracts <i>STARTED</i> nodes of specified type from the list. Method return the empty list if the
     * input list is empty or list doesn't contains the nodes of specified type.
     * @param list
     * @param elementType
     */
    public static <T> List<T> extractNodesOfType(Collection<Node> list, Class<T> elementType)
    {
        return extractNodesOfType(list, elementType, true);
    }
    
    /**
     * Extracts <i>STARTED</i> nodes of specified type from the list. Method return the empty list if the
     * input list is empty or list doesn't contains the nodes of specified type.
     * @param list
     * @param elementType
     * @param startedOnly if <b>true</b> then only started nodes fall into the result list
     */
    public static <T> List<T> extractNodesOfType(Collection<Node> list, Class<T> elementType
            , boolean onlyStarted)
    {
        if (list!=null && !list.isEmpty()){
            List<T> res = new ArrayList<T>(list.size());
            for (Node child: list)
                if (   (Node.Status.STARTED.equals(child.getStatus()) || !onlyStarted)
                    && elementType.isAssignableFrom(child.getClass()))
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
    public static Map<String, NodeAttribute> extractRefereshAttributes(Node node) throws Exception {
        return extractAttributes(node, RefreshAttributeValueHandlerFactory.TYPE);
    }
    
    /**
     * Returns the map of cloned attributes which value handler type is
     * {@link HiddenRefreshAttributeValueHandlerFactory#TYPE}.
     * The ids of attributes are numbers less than zero.
     */
    public static Map<String, NodeAttribute> extractHiddenRefereshAttributes(Node node) 
            throws Exception
    {
        return extractAttributes(node, HiddenRefreshAttributeValueHandlerFactory.TYPE);
    }
    
    public static Map<String, NodeAttribute> concatAttributesMap(Map<String, NodeAttribute> map1
            , Map<String, NodeAttribute> map2)
    {
        if (map1==null) return map2;
        if (map2==null) return map1;
        map1.putAll(map2);
        return map1;
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
    public static Map<String, NodeAttribute> extractAttributes(Node node, String valueHandlerType)
            throws Exception
    {
        Map<String, NodeAttribute> refreshAttributes = new LinkedHashMap<String, NodeAttribute>();
        Collection<NodeAttribute> attrs = node.getAttrs();
        int id=-1;
        if (attrs!=null)
            for (NodeAttribute attr: attrs)
                if (valueHandlerType.equals(attr.getValueHandlerType()))
                {
                    NodeAttribute clone = (NodeAttribute) attr.clone();
                    clone.setOwner(node);
                    clone.setId(id--);
                    Bindings bindings = new SimpleBindings();
//                    bindings.put(ExpressionAttributeValueHandler.NODE_BINDING, new NodeAccessImpl(node));
                    bindings.put(ExpressionAttributeValueHandler.NODE_BINDING, node);
                    Object val = TemplateExpression.eval(clone.getRawValue(), bindings);
                    clone.setRawValue(converter.convert(String.class, val, null));
                    clone.init();
                    refreshAttributes.put(clone.getName(), clone);
                }
        return refreshAttributes.isEmpty()? null : refreshAttributes;
    }

    public static void reconnectDataSources(Node node) 
    {
        try{
            List<Node> childs = node.getSortedChildrens();
            if (childs!=null && !childs.isEmpty())
            {
                Node prevDs = null;
                for (Node child: childs){
                    if (child instanceof DataConsumer)
                    {
                        NodeAttribute autoLinkAttr =
                                child.getNodeAttribute(AbstractDataConsumer.AUTO_LINK_DATA_SOURCE_ATTR);
                        NodeAttribute dsAttr =
                                child.getNodeAttribute(AbstractDataConsumer.DATASOURCE_ATTRIBUTE);
                        if (   autoLinkAttr!=null && dsAttr!=null && prevDs!=null
                            && Boolean.class.equals(autoLinkAttr.getType())
                            && (Boolean)autoLinkAttr.getRealValue()
                            && !ObjectUtils.equals(dsAttr.getRealValue(), prevDs)
                            && ObjectUtils.in(child.getStatus(), Node.Status.INITIALIZED, Node.Status.STARTED))
                        {
                            dsAttr.setValue( (prevDs==null? null : "../"+prevDs.getName()) );
                            dsAttr.save();
                        }
                    }
                    if (child instanceof DataSource)
                        prevDs = child;
                }
            }
        } catch(Exception e) {
            if (node.isLogLevelEnabled(LogLevel.ERROR))
                node.getLogger().error("Automatic data sources linking error", e);
        }
    }
}
