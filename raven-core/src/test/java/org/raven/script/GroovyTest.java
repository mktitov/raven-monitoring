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

package org.raven.script;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyShell;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyTest extends Assert
{
    @Test
    public void test() throws Exception
    {
        Binding binding = new Binding();
        binding.setVariable("foo", new Integer(2));
        GroovyShell shell = new GroovyShell(binding);

        Object value = shell.evaluate("println 'Hello World!'; x = 123; return foo * 10");
        assert value.equals(new Integer(20));
        assert binding.getVariable("x").equals(new Integer(123));
//        ScriptEngineManager factory = new ScriptEngineManager();
//        ScriptEngine engine = factory.getEngineByName("groovy");
//        System.out.println(engine.eval("(1..10).sum()"));
    }
    
    @Test
    public void test_compile() throws Exception
    {
        GroovyClassLoader loader = new GroovyClassLoader();
        Class compiledClass = loader.parseClass("println 'Hello World!'; x = 123; return foo * 10");
        assertNotNull(compiledClass);
        
        GroovyObject obj = (GroovyObject) compiledClass.newInstance();
        obj.invokeMethod("run", null);
    }
}
