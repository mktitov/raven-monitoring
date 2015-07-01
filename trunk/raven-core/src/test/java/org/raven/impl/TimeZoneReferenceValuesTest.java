/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.impl;

import java.util.List;
import java.util.TimeZone;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.impl.ReferenceValueCollectionImpl;

/**
 *
 * @author Mikhail Titov
 */
public class TimeZoneReferenceValuesTest extends RavenCoreTestCase {
    
    @Test
    public void instanceTest() throws Exception {
        NodeAttributeImpl attr1 = new NodeAttributeImpl("name", TimeZone.class, null, null);
        NodeAttributeImpl attr2 = new NodeAttributeImpl("name2", String.class, null, null);
        TimeZoneReferenceValues tzRefValues = new TimeZoneReferenceValues();
        
        ReferenceValueCollectionImpl refValues = new ReferenceValueCollectionImpl(1000, null);
        boolean res = tzRefValues.getReferenceValues(attr2, refValues);
        assertFalse(res);
        assertTrue(refValues.asList().isEmpty());
        
        refValues = new ReferenceValueCollectionImpl(1000, null);
        res = tzRefValues.getReferenceValues(attr1, refValues);
        assertTrue(res);
        List<ReferenceValue> refValuesList = refValues.asList();
        assertFalse(refValuesList.isEmpty());
        ReferenceValue refValue = refValuesList.get(0);
        assertTrue(refValue.getValue() instanceof String);
    }
    
    @Test
    public void serviceTest() {
        NodeAttributeImpl attr1 = new NodeAttributeImpl("name", TimeZone.class, null, null);

        List<ReferenceValue> values = tree.getReferenceValuesForAttribute(attr1);
        assertNotNull(values);
    }
}
