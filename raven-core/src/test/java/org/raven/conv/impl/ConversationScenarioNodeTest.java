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

package org.raven.conv.impl;

import java.util.Collection;
import org.junit.Before;
import org.raven.conv.ConversationScenarioCycleDetectedException;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.conv.ConversationScenarioState;
import org.raven.expr.impl.IfNode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationScenarioNodeTest extends RavenCoreTestCase
{
    private ConversationScenarioNode conversation;
    private ConversationScenarioState state;

    @Before
    public void prepare() throws ConversationScenarioCycleDetectedException
    {
        conversation = new ConversationScenarioNode();
        conversation.setName("conversation");
        tree.getRootNode().addAndSaveChildren(conversation);
        assertTrue(conversation.start());

        state = conversation.createConversationState();
    }

    @Test
    public void createConversationState() throws ConversationScenarioCycleDetectedException
    {
        assertNotNull(conversation.createConversationState());
    }

    @Test
    public void stayOnTheCurrentPointTest() throws Exception
    {
        Collection<Node> actions = conversation.makeConversation(state);
        assertNotNull(actions);
        assertSame(conversation, state.getNextConversationPoint());
    }

    @Test
    public void changeConversationPointTest() throws Exception
    {
        ConversationScenarioPointNode point = addPoint(conversation, "point");
        conversation.makeConversation(state);
        assertSame(point, state.getNextConversationPoint());
    }

    @Test
    public void conversationPointNextPointTest() throws Exception
    {
        ConversationScenarioPointNode point = addPoint(conversation, "point");
        Node action = addAction(point, "action");
        conversation.setNextPoint(point);

        Collection<Node> actions = conversation.makeConversation(state);
        assertSame(conversation, state.getNextConversationPoint());
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertSame(action, actions.iterator().next());
    }

    @Test
    public void disabledPointTest() throws Exception
    {
        ConversationScenarioPointNode point = addPoint(conversation, "point");
        point.stop();
        conversation.makeConversation(state);
        assertSame(conversation, state.getNextConversationPoint());
    }

    @Test
    public void actionsTest() throws Exception
    {
        Node action1 = addAction(conversation, "action1");
        Node action2 = addAction(conversation, "action2");
        action2.stop();
        IfNode if1 = addIf(conversation, "if1", "dtmf==1");
        Node action3 = addAction(if1, "action3");
        IfNode if2 = addIf(conversation, "if2", "dtmf==2");
        Node action4 = addAction(if2, "action4");

        state.getBindings().put("dtmf", 1);
        Collection<Node> actions = conversation.makeConversation(state);
        assertNotNull(actions);
        assertEquals(2, actions.size());
        Object[] actionsArr = actions.toArray();
        assertSame(action1, actionsArr[0]);
        assertSame(action3, actionsArr[1]);
    }

    private ConversationScenarioPointNode addPoint(Node parent, String name)
    {
        ConversationScenarioPointNode point = new ConversationScenarioPointNode();
        point.setName("point");
        parent.addAndSaveChildren(point);
        assertTrue(point.start());
        return point;
    }

    private Node addAction(Node  point, String name)
    {
        Node action = new BaseNode(name);
        point.addAndSaveChildren(action);
        assertTrue(action.start());
        return action;
    }

    private IfNode addIf(Node parent, String name, String expression) throws Exception
    {
        IfNode ifNode = new IfNode();
        ifNode.setName(name);
        parent.addAndSaveChildren(ifNode);
        ifNode.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue(expression);
        ifNode.setUsedInTemplate(false);
        assertTrue(ifNode.start());
        return ifNode;
    }
}