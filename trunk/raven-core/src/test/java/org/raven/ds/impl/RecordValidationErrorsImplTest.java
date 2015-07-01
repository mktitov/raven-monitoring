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

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class RecordValidationErrorsImplTest extends Assert
{
    @Test
    public void toTextTest()
    {
        RecordValidationErrorsImpl errors = new RecordValidationErrorsImpl("test");
        assertNull(errors.toText());

        errors.addValidationErrors("field1", Arrays.asList("error1", "error2"));
        errors.addValidationErrors("field2", Arrays.asList("error1"));
        assertEquals("Record of schema (test) has validation errors: \nfield1:\n  error1\n  error2\nfield2:\n  error1\n", errors.toText());
    }
}