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

import java.nio.charset.Charset;
import java.util.List;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.constraints.impl.ReferenceValueCollectionImpl;

/**
 *
 * @author Mikhail Titov
 */
public class CharsetReferenceValuesTest extends RavenCoreTestCase
{
    @Test
    public void referenceValuesTest() throws TooManyReferenceValuesException
    {
        NodeAttributeImpl attr1 = new NodeAttributeImpl("name", Charset.class, null, null);
        NodeAttributeImpl attr2 = new NodeAttributeImpl("name2", String.class, null, null);
        CharsetReferenceValues charsetRefValues = new CharsetReferenceValues();
        
        ReferenceValueCollectionImpl refValues = new ReferenceValueCollectionImpl(1000, null);
        boolean res = charsetRefValues.getReferenceValues(attr2, refValues);
        assertFalse(res);
        assertTrue(refValues.asList().isEmpty());
        
        refValues = new ReferenceValueCollectionImpl(1000, null);
        res = charsetRefValues.getReferenceValues(attr1, refValues);
        assertTrue(res);
        List<ReferenceValue> refValuesList = refValues.asList();
        assertFalse(refValuesList.isEmpty());
        ReferenceValue refValue = refValuesList.get(0);
        assertTrue(refValue.getValue() instanceof String);
    }

    @Test
    public void inServiceTest()
    {
        NodeAttributeImpl attr1 = new NodeAttributeImpl("name", Charset.class, null, null);

        List<ReferenceValue> values = tree.getReferenceValuesForAttribute(attr1);
        assertNotNull(values);
    }
}