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

package org.raven.impl;

import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.SystemNode;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class StringToNodeConverterTest extends ServiceTestCase
{

    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void test()
    {
        TypeConverter converter = registry.getService(TypeConverter.class);
        Node systemNode = converter.convert(Node.class, SystemNode.NAME, null);
        
        assertNotNull(systemNode);
        assertEquals(SystemNode.NAME, systemNode.getName());
    }
}
