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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;
import org.raven.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class ParameterNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        ParameterNode param = new ParameterNode();
        param.setName("param");
        tree.getRootNode().addAndSaveChildren(param);
        param.setParameterValue("1");
        param.setConvertToType(null);
        assertTrue(param.start());

        assertEquals("1", param.getValue());

        param.setConvertToType(Integer.class);
        assertEquals(1, param.getValue());

        param.setConvertToType(Date.class);
        param.setPattern("dd.MM.yyyy");
        param.setParameterValue("01.01.2009");

        Date date = new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2009");
        assertEquals(date, param.getValue());
    }
}