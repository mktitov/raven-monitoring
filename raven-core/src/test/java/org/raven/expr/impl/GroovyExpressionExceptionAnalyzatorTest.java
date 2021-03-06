/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.expr.impl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.text.SimpleTemplateEngine;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionInfo;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class GroovyExpressionExceptionAnalyzatorTest extends RavenCoreTestCase {
    private BaseNode node;
    private NodeAttributeImpl attr1;
    private IMocksControl mocks;
    private GroovyExpressionCompiler compiler;
    private String ident;
    private SimpleBindings bindings;
    
    @Before
    public void prepare() throws Exception {
        mocks = createControl();
        compiler = new GroovyExpressionCompiler(new ExpressionCacheImpl());
        ident = GroovyExpressionCompiler.convertToIdentificator("test_script");
        bindings = new SimpleBindings();
        node = new BaseNode("test node");
        testsNode.addAndSaveChildren(node);
        assertTrue(node.start());
    }
    
    @Test 
    public void boundsTest() throws Exception {
        check(
            "throw new Exception('Test exception')", 
            ">>> [ 1] (  1) throw new Exception('Test exception')\n"
        );
    }
    
    @Test 
    public void emptyLinesTest() throws Exception {
        check(
            "\nthrow new Exception('Test exception')", 
              "         (  1) \n"
            + ">>> [ 1] (  2) throw new Exception('Test exception')\n"
        );
    }
    
    @Test 
    public void linesBeforeAndAfterTest() throws Exception {
        check(
            "1\n2\nthrow new Exception('Test exception')\n4", 
              "         (  2) 2\n"
            + ">>> [ 1] (  3) throw new Exception('Test exception')\n"
            + "         (  4) 4\n"
        );
    }
    
    @Test 
    public void twoErrorsTest() throws Exception {
        check(
            "a = {throw new Exception('Test exception')}\n2\n3\n4\na()", 
              ">>> [ 1] (  1) a = {throw new Exception('Test exception')}\n"
            + "         (  2) 2\n"
            + "...\n"
            + "         (  4) 4\n"
            + ">>> [ 2] (  5) a()\n"
        );
    }
    
    @Test 
    public void twoErrorsNearTest() throws Exception {
        check(
            "a = {throw new Exception('Test exception')}\n2\n3\na()", 
              ">>> [ 1] (  1) a = {throw new Exception('Test exception')}\n"
            + "         (  2) 2\n"
            + "         (  3) 3\n"
            + ">>> [ 2] (  4) a()\n"
        );
    }
    
    @Test 
    public void twoErrorsMergeTest1() throws Exception {
        check(
            "a = {throw new Exception('Test exception')}\na()", 
              ">>> [ 1] (  1) a = {throw new Exception('Test exception')}\n"
            + ">>> [ 2] (  2) a()\n"
        );
    }
    
    @Test 
    public void twoErrorsMergeTest2() throws Exception {
        check(
            "a = {throw new Exception('Test exception')};a()", 
              ">>> [ 1] (  1) a = {throw new Exception('Test exception')};a()\n"
        );
    }
    
    @Test 
    public void twoErrorsMergeTest3() throws Exception {
        check(
            "[1].each{\nthrow new Exception('Test exception')\n}", 
              ">>> [ 2] (  1) [1].each{\n"
            + ">>> [ 1] (  2) throw new Exception('Test exception')\n"
            + "         (  3) }\n"
        );
    }
    
    @Test
    public void execExprFromExprTest1() throws Exception {
        Node node1 = new BaseNode("node1");
        testsNode.addAndSaveChildren(node1);
        addAttr(node1, "expr1", "cl()");
        
        Node node2 = new BaseNode("node2");
        testsNode.addAndSaveChildren(node2);
        NodeAttribute attr = addAttr(node2, "expr2", "cl = {\n"
                + "throw new Exception('test error')\n"
                + "}\n"
                + "node.parent.getNode('node1').$expr1(cl:cl)");
        attr.getRealValue();
    }
    
//    @Test
    public void execExprFromExprTest2() throws Exception {
        Node node1 = new BaseNode("node1");
        testsNode.addAndSaveChildren(node1);
        addAttr(node1, "expr1", 
                "{->\n"
                + "throw new Exception('test exception')\n"
                + "}");
        
        Node node2 = new BaseNode("node2");
        testsNode.addAndSaveChildren(node2);
        NodeAttribute attr = addAttr(node2, "expr2", 
                "cl = node.parent.getNode('node1').$expr1\n"
                + "cl()");
        attr.getRealValue();
    }
    
    @Test
    public void execGroovyTemplate() throws Exception {
        SimpleTemplateEngine engine = new SimpleTemplateEngine();
        String source = "line 1\n"
                + "line 2\n"
                + "line 3\n"
                + "<%\n"
                + "{->\n"
                + "  throw new Exception('test')\n"
                + "}()\n"
                + "%>\n"
                + "line 5";
        Binding binding = new Binding();
        binding.setVariable("engine", engine);
        String scriptId = 
                "SimpleTemplateScript"
                + new GroovyShell(binding).evaluate("engine.counter")
                + ".groovy";
        try {
            engine.createTemplate(source).make().toString();
        } catch (Throwable e) {
            GroovyExpressionExceptionAnalyzator a = new GroovyExpressionExceptionAnalyzator(scriptId, e, 1);
            ExpressionInfoImpl exprInfo = new ExpressionInfoImpl("attr", node, source);
            Map<String, ExpressionInfo> sources = new HashMap<String, ExpressionInfo>();
            sources.put(scriptId, exprInfo);
            Collection<MessageConstructor> messageConstructors = a.getMessageConstructors(sources);
            String res = messageConstructors.iterator().next().constructMessage("", new StringBuilder()).toString();
            System.out.println("MESSAGE:");
            System.out.println(res);
        }
    }
    
    
    private NodeAttribute addAttr(Node owner, String name, String expr) throws Exception {
        NodeAttributeImpl attr = new NodeAttributeImpl(name, String.class, expr, null);
        attr.setOwner(owner);
        attr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
        attr.init();
        owner.addAttr(attr);
        return attr;
    }
    
    private void check(String source, String expectedRes) {
        try {
            compileAndExecute(source);
        } catch (ScriptException e) {
            checkAnalyzer(e, source, "Exception at @attr (/\"tests node\"/\"test node\"/)\nMessage: Test exception\nCause: java.lang.Exception\n...\n"+expectedRes);
        }
        
    }
    
    private Object compileAndExecute(String source) throws ScriptException  {
        Expression expr = compiler.compile(source, GroovyExpressionCompiler.LANGUAGE, ident);
        return expr.eval(bindings);
    }
    
    private void checkAnalyzer(Throwable e, String source, String expectedRes) {
        GroovyExpressionExceptionAnalyzator a = new GroovyExpressionExceptionAnalyzator(ident, e, 1);
        ExpressionInfoImpl exprInfo = new ExpressionInfoImpl("attr", node, source);
        Map<String, ExpressionInfo> sources = new HashMap<String, ExpressionInfo>();
        sources.put(ident, exprInfo);
        Collection<MessageConstructor> messageConstructors = a.getMessageConstructors(sources);
        assertEquals(1, messageConstructors.size());
        String res = messageConstructors.iterator().next().constructMessage("", new StringBuilder()).toString();
//        String res = a.addResultToBuilder("", new StringBuilder()).toString();
        if (!expectedRes.equals(res)) {
            System.out.println("EXPECTED: ");
            System.out.println(expectedRes);
        }
        System.out.println("\nRETURNED");
        System.out.println(res);
        assertEquals(expectedRes, res);
    }
}
