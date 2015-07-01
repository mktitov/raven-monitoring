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

import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class MinLengthValidatorNodeTest extends RavenCoreTestCase
{
    private MinLengthValidatorNode validator;

    @Before
    public void prepare()
    {
        validator = new MinLengthValidatorNode();
        validator.setName("validator");
        tree.getRootNode().addAndSaveChildren(validator);
        validator.setMinLength(2);
        assertTrue(validator.start());
    }

    @Test
    public void nullValueTest()
    {
        assertNotNull(validator.validate(null));
    }

    @Test
    public void lessThanTwoValueTest()
    {
        String error = validator.validate("t");
        assertNotNull(error);
        assertNotNull(validator.validate(1));
    }

    @Test
    public void moreThanTwoValueTest()
    {
        assertNull(validator.validate("te"));
        assertNull(validator.validate(10));
        assertNull(validator.validate("test"));
        assertNull(validator.validate(100));
    }

}