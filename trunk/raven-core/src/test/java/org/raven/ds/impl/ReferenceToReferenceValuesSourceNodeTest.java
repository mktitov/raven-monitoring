/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.ds.impl;

import java.util.List;
import org.junit.Test;
import org.raven.ds.ReferenceValuesSource;
import org.raven.test.RavenCoreTestCase;
import org.weda.constraints.ReferenceValue;

/**
 *
 * @author Mikhail Titov
 */
public class ReferenceToReferenceValuesSourceNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        CustomReferenceValuesSourceNode source = new CustomReferenceValuesSourceNode();
        source.setName("source");
        tree.getRootNode().addAndSaveChildren(source);
        source.setReferenceValuesExpression("[1:'one']");
        assertTrue(source.start());

        ReferenceToReferenceValuesSourceNode ref = new ReferenceToReferenceValuesSourceNode();
        ref.setName("reference");
        tree.getRootNode().addAndSaveChildren(ref);
        ref.setReferenceValuesSource(source);
        assertTrue(ref.start());

        ReferenceValuesSource valuesSource = ref.getReferenceValuesSource();
        assertNotNull(valuesSource);
        List<ReferenceValue> values = valuesSource.getReferenceValues();
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals(1, values.get(0).getValue());
        assertEquals("one", values.get(0).getValueAsString());
    }
}