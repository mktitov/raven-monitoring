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

package org.raven.ds.impl;

import java.util.List;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;

/**
 *
 * @author Mikhail Titov
 */
public class DataPipeConvertToTypesReferenceValuesTest extends RavenCoreTestCase
{
    @Test
    public void test() throws TooManyReferenceValuesException
    {
        DataPipeImpl dataPipe = new DataPipeImpl();
        dataPipe.setName("pipe");
        tree.getRootNode().addChildren(dataPipe);
        dataPipe.save();
        dataPipe.init();
        
        List<ReferenceValue> refValues = dataPipe.getNodeAttribute(
                DataPipeImpl.CONVERT_VALUE_TO_TYPE_ATTRIBUTE).getReferenceValues();
        assertNotNull(refValues);
        assertEquals(6, refValues.size());
        assertEquals(refValues.get(0).getValue(), String.class.getName());
        assertEquals(refValues.get(0).getValueAsString(), "String");
        assertEquals(refValues.get(1).getValue(), Long.class.getName());
        assertEquals(refValues.get(1).getValueAsString(), "Long");
        assertEquals(refValues.get(2).getValue(), Integer.class.getName());
        assertEquals(refValues.get(2).getValueAsString(), "Integer");
        assertEquals(refValues.get(3).getValue(), Short.class.getName());
        assertEquals(refValues.get(3).getValueAsString(), "Short");
        assertEquals(refValues.get(4).getValue(), Double.class.getName());
        assertEquals(refValues.get(4).getValueAsString(), "Double");
        assertEquals(refValues.get(5).getValue(), Float.class.getName());
        assertEquals(refValues.get(5).getValueAsString(), "Float");
    }
}
