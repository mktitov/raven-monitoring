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
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueCollectionImpl;

/**
 *
 * @author Mikhail Titov
 */
public class EnumReferenceValuesTest extends RavenCoreTestCase
{

    public enum TestEnum {ONE, TWO};
    
    private NodeAttribute attr;
    
    @Before
    public void setupTest()
    {
        store.removeNodes();
        
        attr = new NodeAttributeImpl("attr", TestEnum.class, null, null);
    }
    
    @Test
    public void instanceTest() throws TooManyReferenceValuesException
    {
        EnumReferenceValues enumReferenceValues = new EnumReferenceValues();
        ReferenceValueCollection values = new ReferenceValueCollectionImpl(Integer.MAX_VALUE, null);
        assertTrue(enumReferenceValues.getReferenceValues(attr, values));
        checkValues(values.asList());
    }
    
    @Test
    public void treeTest()
    {
        List<ReferenceValue> values = tree.getReferenceValuesForAttribute(attr);
        
        checkValues(values);
    }
    
    private void checkValues(List<ReferenceValue> values)
    {
        assertNotNull(values);
        assertEquals(2, values.size());
        assertEquals("ONE", values.get(0).getValue());
        assertEquals("TWO", values.get(1).getValue());
    }
}
