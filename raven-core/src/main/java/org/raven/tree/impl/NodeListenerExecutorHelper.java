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
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.weda.beans.GetOperation;
import org.weda.beans.ObjectUtils;
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
            
    public static Object getOldValue(Object obj, String propertyName)
    {
        GetOperation getter = compiler.compileGetOperation(obj.getClass(), propertyName);
        return getter.getValue(obj);
    }
    
    public static void fireNodeAttributeValueChanged(
            Node node, String attributeName, Object oldValue, Object newValue)
    {
        if (node.getListeners()!=null && !ObjectUtils.equals(oldValue, newValue))
        {
            NodeAttribute attr = node.getNodeAttribute(attributeName);
            String pattern = 
                    classDescriptorRegistry.getPropertyDescriptor(
                        node.getClass(), attributeName).getPattern();
            ((BaseNode)node).fireAttributeValueChanged(
                    (NodeAttributeImpl) attr, converter.convert(String.class, oldValue, pattern));
//            for (NodeListener listener: node.getListeners())
//                listener.nodeAttributeValueChanged(
//                    node, attr, 
//                    converter.convert(String.class, oldValue, pattern),
//                    converter.convert(String.class, newValue, pattern));
        }
    }
}
