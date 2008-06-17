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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.tree.FactoryNotFoundException;
import org.raven.tree.NodeAttribute;
import org.weda.constraints.ReferenceValue;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class AttributeValueHandlerRegistryImplTest extends Assert
{
    @Test
    public void test() throws FactoryNotFoundException
    {
        AttributeValueHandlerFactory factory = createMock(AttributeValueHandlerFactory.class);
        AttributeValueHandler handler = createMock(AttributeValueHandler.class);
        NodeAttribute attribute = createMock(NodeAttribute.class);
        
        expect(factory.getName()).andReturn("factory_name");
        expect(factory.createValueHandler(attribute)).andReturn(handler);
        
        replay(factory, handler, attribute);
        
        Map<String, AttributeValueHandlerFactory> factories = 
                new HashMap<String, AttributeValueHandlerFactory>();
        factories.put("handlerType", factory);
        
        AttributeValueHandlerRegistryImpl reg = new AttributeValueHandlerRegistryImpl(factories);
        
        List<ReferenceValue> types = reg.getValueHandlerTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals("handlerType", types.get(0).getValue());
        assertEquals("factory_name", types.get(0).getValueAsString());
        
        AttributeValueHandler valueHandler = reg.getValueHandler("handlerType", attribute);
        assertNotNull(valueHandler);
        assertSame(handler, valueHandler);
        
        try{
            valueHandler = reg.getValueHandler("undefined_type", attribute);
            fail();
        }catch(FactoryNotFoundException e)
        {
        }
        
        verify(factory, handler, attribute);
    }
}
