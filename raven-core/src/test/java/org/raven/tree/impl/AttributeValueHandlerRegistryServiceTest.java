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

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.AttributeValueHandlerRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class AttributeValueHandlerRegistryServiceTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        AttributeValueHandlerRegistry valueHandlerRegistry = 
                registry.getService(AttributeValueHandlerRegistry.class);
        assertNotNull(valueHandlerRegistry);
        assertNotNull(valueHandlerRegistry.getValueHandlerTypes());
    }
}
