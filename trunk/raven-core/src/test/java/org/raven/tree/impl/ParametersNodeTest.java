/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.tree.impl;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class ParametersNodeTest extends RavenCoreTestCase
{
    private ParametersNode parameters;

    @Before
    public void prepareTest()
    {
        parameters = new ParametersNode();
        tree.getRootNode().addAndSaveChildren(parameters);
        assertTrue(parameters.start());
    }

    @Test
    public void noParametersTest()
    {
        assertNull(parameters.getParameterValues());

        ParameterNode parameter = new ParameterNode();
        parameter.setName("param");
        parameters.addAndSaveChildren(parameter);

        assertNull(parameters.getParameterValues());
    }

    @Test
    public void hasParametersTest()
    {
        ParameterNode parameter = new ParameterNode();
        parameter.setName("param1");
        parameters.addAndSaveChildren(parameter);
        parameter.setParameterValue("1");
        assertTrue(parameter.start());
        
        parameter = new ParameterNode();
        parameter.setName("param2");
        parameters.addAndSaveChildren(parameter);
        parameter.setParameterValue("1");
        parameter.setConvertToType(Integer.class);
        assertTrue(parameter.start());

        Map<String, Object> paramValues = parameters.getParameterValues();
        assertNotNull(paramValues);
        assertEquals(2, paramValues.size());

        assertEquals("1", paramValues.get("param1"));
        assertEquals(1, paramValues.get("param2"));
    }
}