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
public class RegexpValidatorNodeTest extends RavenCoreTestCase
{
    RegexpValidatorNode validator;

    @Before
    public void prepare()
    {
        validator = new RegexpValidatorNode();
        validator.setName("validator");
        tree.getRootNode().addAndSaveChildren(validator);
        validator.setRegexp("\\d\\d");
        assertTrue(validator.start());
    }

    @Test
    public void nullValueTest()
    {
        assertNotNull(validator.validate(null));
    }

    @Test
    public void successTest()
    {
        assertNull(validator.validate("12"));
    }

    @Test
    public void unsuccessTest()
    {
        String error = validator.validate("123");
        assertNotNull(error);        
    }

    @Test
    public void regexpChangeTest()
    {
        validator.setRegexp("\\d\\d\\d");
        assertNull(validator.validate("123"));
        assertNotNull(validator.validate("12"));
    }
}