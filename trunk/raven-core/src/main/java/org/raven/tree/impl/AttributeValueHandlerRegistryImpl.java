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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.tree.AttributeValueHandlerRegistry;
import org.raven.tree.FactoryNotFoundException;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.impl.ReferenceValueImpl;

/**
 *
 * @author Mikhail Titov
 */
public class AttributeValueHandlerRegistryImpl implements AttributeValueHandlerRegistry
{
    private final Map<String, AttributeValueHandlerFactory> factories;

    public AttributeValueHandlerRegistryImpl(Map<String, AttributeValueHandlerFactory> factories)
    {
        this.factories = factories;
    }

    public List<ReferenceValue> getValueHandlerTypes()
    {
        List<ReferenceValue> types = new ArrayList<ReferenceValue>(factories.size());
        for (Map.Entry<String, AttributeValueHandlerFactory> entry: factories.entrySet())
            types.add(new ReferenceValueImpl(entry.getKey(), entry.getValue().getName()));
        
        return types;
    }

    public AttributeValueHandler getValueHandler(String valueHandlerType, NodeAttribute attribute) 
            throws FactoryNotFoundException
    {
        AttributeValueHandlerFactory factory = factories.get(valueHandlerType);
        if (factory==null)
            throw new FactoryNotFoundException(valueHandlerType);
        return factory.createValueHandler(attribute);
    }

}
