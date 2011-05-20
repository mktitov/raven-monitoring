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

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class CustomValueValidatorNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        CustomValueValidatorNode validator = new CustomValueValidatorNode();
        validator.setName("validator");
        tree.getRootNode().addAndSaveChildren(validator);
        assertTrue(validator.start());

        assertNull(validator.validate("test"));
        
        validator.setValidateExpression("'validation '+value");
        assertEquals("validation test", validator.validate("test"));
    }
}