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

package org.raven.template.impl;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;
import org.raven.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TemplateExpressionTest extends RavenCoreTestCase
{
    @Test
    public void noExpressionTest() throws ScriptException
    {
        Object val = TemplateExpression.eval("test", null);
        assertNotNull(val);
        assertEquals("test", val);
    }

    @Test
    public void ExpressionTest() throws ScriptException
    {
        Object val = TemplateExpression.eval("^t 1+1", null);
        assertNotNull(val);
        assertEquals(2, val);
    }

    @Test
    public void ExpressionWithBindingsTest() throws ScriptException
    {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("var", 1);
        Object val = TemplateExpression.eval("^t 1+var", bindings);
        assertNotNull(val);
        assertEquals(2, val);
    }
}