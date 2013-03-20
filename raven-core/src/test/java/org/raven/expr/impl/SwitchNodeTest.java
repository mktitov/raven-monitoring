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

import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class SwitchNodeTest extends RavenCoreTestCase {
    private SwitchNode switchNode;
    
    @Before
    public void prepare() {
        switchNode = new SwitchNode();
        switchNode.setName("switch");
        testsNode.addAndSaveChildren(switchNode);
        assertTrue(switchNode.start());
    }
    
    @Test
    public void woAnyConditions() {
        assertNull(switchNode.getEffectiveNodes());
    }
    
    @Test
    public void conditionsTest() throws Exception {
        createConditions();
        checkCondition("1", "node1");
        checkCondition("2", "node2");
        checkCondition("3", "node3");
    }
    
    private void checkCondition(String value, String expectedChildNodeName) {
        switchNode.setValue(value);
        Collection<Node> childs = switchNode.getEffectiveNodes();
        assertNotNull(childs);
        assertEquals(1, childs.size());
        assertEquals(expectedChildNodeName, childs.iterator().next().getName());
    }
    
    private void createConditions() throws Exception {
        addCondition("c1", "value==1", "node1");
        addCondition("c2", "value==2", "node2");
        DefaultCondition defNode = new DefaultCondition();
        defNode.setName("default");
        switchNode.addAndSaveChildren(defNode);
        assertTrue(defNode.start());
        addChildNode(defNode, "node3");
    }
    
    private void addCondition(String name, String condition, String childNodeName) throws Exception {
        ExpressionConditionNode conditionNode = new ExpressionConditionNode();
        conditionNode.setName(name);
        switchNode.addAndSaveChildren(conditionNode);
        conditionNode.getAttr(ExpressionConditionNode.CONDITION_ATTR).setValue(condition);
        assertTrue(conditionNode.start());
        addChildNode(conditionNode, childNodeName);
    }
    
    private void addChildNode(Node owner, String childNodeName) {
        BaseNode childNode = new BaseNode(childNodeName);
        owner.addAndSaveChildren(childNode);
        assertTrue(childNode.start());
    }
}
