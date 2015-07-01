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
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Titov
 */
public class MaxLengthValidatorNodeTest extends RavenCoreTestCase
{
    private MaxLengthValidatorNode validator;

    @Before
    public void prepare()
    {
        validator = new MaxLengthValidatorNode();
        validator.setName("validator");
        tree.getRootNode().addAndSaveChildren(validator);
        validator.setMaxLength(2);
        assertTrue(validator.start());
    }

    @Test
    public void nullValueTest()
    {
        assertNull(validator.validate(null));
    }

    @Test
    public void moreThanTwoValueTest()
    {
        String error = validator.validate("tes");
        assertNotNull(error);
        assertNotNull(validator.validate(123));
    }

    @Test
    public void lessThanTwoValueTest()
    {
        assertNull(validator.validate("te"));
        assertNull(validator.validate(10));
        assertNull(validator.validate("t"));
        assertNull(validator.validate(1));
    }

}