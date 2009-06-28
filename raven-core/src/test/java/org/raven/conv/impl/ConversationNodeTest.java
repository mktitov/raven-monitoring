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
import org.raven.conv.ConversationCycleDetectedException;
import org.raven.conv.impl.ConversationNode;
import org.raven.conv.impl.ConversationPointNode;
import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.raven.conv.ConversationState;
import org.raven.expr.impl.IfNode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationNodeTest extends RavenCoreTestCase
{
    private ConversationNode conversation;
    private ConversationState state;

    @Before
    public void prepare() throws ConversationCycleDetectedException
    {
        conversation = new ConversationNode();
        conversation.setName("conversation");
        tree.getRootNode().addAndSaveChildren(conversation);
        assertTrue(conversation.start());

        state = conversation.createConversationState();
    }

    @Test
    public void createConversationState() throws ConversationCycleDetectedException
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
        ConversationPointNode point = addPoint(conversation, "point");
        conversation.makeConversation(state);
        assertSame(point, state.getNextConversationPoint());
    }

    @Test
    public void conversationPointNextPointTest() throws Exception
    {
        ConversationPointNode point = addPoint(conversation, "point");
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
        ConversationPointNode point = addPoint(conversation, "point");
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

//    @Test
    public void test() throws Exception
    {
        ConversationNode requestHandler = new ConversationNode();
        requestHandler.setName("requestHandler");
        tree.getRootNode().addAndSaveChildren(requestHandler);
        assertTrue(requestHandler.start());

        BaseNode hello = new BaseNode("hello");
        requestHandler.addAndSaveChildren(hello);
        assertTrue(hello.start());

        ConversationPointNode helloEntry = new ConversationPointNode();
        helloEntry.setName("helloEntry");
        requestHandler.addAndSaveChildren(helloEntry);
        assertTrue(hello.start());

        IfNode if1 = new IfNode();
        if1.setName("dtmf==1");
        helloEntry.addAndSaveChildren(if1);
        if1.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE).setValue("dtmf==1");
        if1.setUsedInTemplate(false);
        assertTrue(if1.start());

        BaseNode balance = new BaseNode("your balance is zero");
//        if
    }

    private ConversationPointNode addPoint(Node parent, String name)
    {
        ConversationPointNode point = new ConversationPointNode();
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