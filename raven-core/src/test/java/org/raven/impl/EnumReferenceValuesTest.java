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

import java.util.List;
import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class EnumReferenceValuesTest extends ServiceTestCase
{

    public enum TestEnum {ONE, TWO};
    
    private NodeAttribute attr;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        attr = new NodeAttributeImpl("attr", TestEnum.class, null, null);
    }
    
    @Test
    public void instanceTest()
    {
        EnumReferenceValues referenceValues = new EnumReferenceValues();
        List<String> values = referenceValues.getReferenceValues(attr);
        
        checkValues(values);
    }
    
    @Test
    public void treeTest()
    {
        Tree tree = registry.getService(Tree.class);
        List<String> values = tree.getReferenceValuesForAttribute(attr);
        
        checkValues(values);
    }
    
    private void checkValues(List<String> values)
    {
        assertNotNull(values);
        assertEquals(2, values.size());
        assertEquals("ONE", values.get(0));
        assertEquals("TWO", values.get(1));
    }
}
