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

package org.raven.tree.impl;

import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.weda.beans.GetOperation;
import org.weda.beans.ObjectUtils;
import org.weda.beans.PropertyDescriptor;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import org.weda.services.PropertyOperationCompiler;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeListenerExecutorHelper
{
    @Service
    private static TypeConverter converter;
    @Service
    private static ClassDescriptorRegistry classDescriptorRegistry;
    @Service
    private static PropertyOperationCompiler compiler;
    
    public static Object getParameterValue(Node node, String parameterName, Object fieldValue)
    {
        NodeAttribute attr = node.getNodeAttribute(parameterName);
        return attr.getRealValue();
//        if (attr==null || !attr.isAttributeReference())
//        {
//            if (fieldValue==null)
//            {
//                PropertyDescriptor desc = classDescriptorRegistry.getPropertyDescriptor(
//                        node.getClass(), parameterName);
//                return converter.convert(
//                        desc.getType(), node.getParentAttributeRealValue(parameterName)
//                        , desc.getPattern());
//            } else
//                return fieldValue;
//        } 
//        else 
//        {
//            PropertyDescriptor desc = 
//                    classDescriptorRegistry.getPropertyDescriptor(node.getClass(), parameterName);
//            return converter.convert(desc.getType(), attr.getRealValue(), desc.getPattern());
//        }
    }
    
    public static Object getParentAttributeValue(
            Node node, String parameterName, Class parameterType)
    {
        Object val = node.getParentAttributeRealValue(parameterName);
        if (val==null)
            return null;
        else
        {
            PropertyDescriptor desc = 
                    classDescriptorRegistry.getPropertyDescriptor(node.getClass(), parameterName);
            return converter.convert(parameterType, val, desc.getPattern());
        }
    }
    
    public static Object getOldValue(Object obj, String propertyName)
    {
        GetOperation getter = compiler.compileGetOperation(obj.getClass(), propertyName);
        return getter.getValue(obj);
    }
    
    public static void fireNodeAttributeValueChanged(
            Node node, String attributeName, Object oldValue, Object newValue)
    {
        throw new UnsupportedOperationException(String.format(
                "Error seting value for field (%s) of the class (%s). " +
                "Set operation not supported for parameters"
                , attributeName, node.getClass().getName()));
//        throw new ;
//        if (node.getStatus()!=Status.CREATED && !ObjectUtils.equals(oldValue, newValue))
//        {
//            NodeAttribute attr = node.getNodeAttribute(attributeName);            
//            String pattern = 
//                    classDescriptorRegistry.getPropertyDescriptor(
//                        node.getClass(), attributeName).getPattern();
//            String newStrValue = converter.convert(String.class, newValue, pattern);
//            attr.setRawValue(newStrValue);
//            
//            if (node.getListeners()!=null)
//                ((BaseNode)node).fireAttributeValueChanged(
//                        (NodeAttributeImpl) attr
//                        , converter.convert(String.class, oldValue, pattern)
//                        , newStrValue);
//            for (NodeListener listener: node.getListeners())
//                listener.nodeAttributeValueChanged(
//                    node, attr, 
//                    converter.convert(String.class, oldValue, pattern),
//                    converter.convert(String.class, newValue, pattern));
//        }
    }
}
