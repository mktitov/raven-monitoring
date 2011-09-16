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

import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class ConversationScenarioStateImplTest extends RavenCoreTestCase
{
    private ConversationScenarioStateImpl state;

    @Before
    public void prepare()
    {
        state = new ConversationScenarioStateImpl();
    }

    @Test
    public void getBindingsTest()
    {
        assertNotNull(state.getBindings());
        state.getBindings().put("test", "value");
        assertEquals("value", state.getBindings().get("test"));
    }

//    @Test
//    public void setNextConversationPointTest() throws ConversationScenarioCycleDetectedException
//    {
//        ConversationScenarioPoint point = new ConversationScenarioPointNode();
//        point.setName("point");
//        tree.getRootNode().addAndSaveChildren(point);
//        assertTrue(point.start());
//        assertFalse(point.getImmediateTransition());
//        state.setNextConversationPoint(point);
//        assertSame(point, state.getConversationPoint());
//        assertFalse(state.hasImmediateTransition());
//    }
//
//    @Test
//    public void immediateTransitionTest() throws ConversationScenarioCycleDetectedException
//    {
//        ConversationScenarioPointNode point = new ConversationScenarioPointNode();
//        point.setName("point");
//        tree.getRootNode().addAndSaveChildren(point);
//        point.setImmediateTransition(true);
//        assertTrue(point.start());
//        assertTrue(point.getImmediateTransition());
//        state.setNextConversationPoint(point);
//        assertSame(point, state.getConversationPoint());
//        assertTrue(state.hasImmediateTransition());
//    }
//
//    @Test(expected=ConversationScenarioCycleDetectedException.class)
//    public void loopDetectionTest1() throws ConversationScenarioCycleDetectedException
//    {
//        ConversationScenarioPointNode point = new ConversationScenarioPointNode();
//        point.setName("point");
//        tree.getRootNode().addAndSaveChildren(point);
//        point.setImmediateTransition(true);
//        assertTrue(point.start());
//        assertTrue(point.getImmediateTransition());
//        state.setNextConversationPoint(point);
//        state.setNextConversationPoint(point);
//    }
//
//    @Test()
//    public void loopDetectionTest2() throws ConversationScenarioCycleDetectedException
//    {
//        ConversationScenarioPointNode point = new ConversationScenarioPointNode();
//        point.setName("point");
//        tree.getRootNode().addAndSaveChildren(point);
//        point.setImmediateTransition(true);
//        assertTrue(point.start());
//        ConversationScenarioPointNode point2 = new ConversationScenarioPointNode();
//        point2.setName("point2");
//        tree.getRootNode().addAndSaveChildren(point2);
//        point2.setImmediateTransition(false);
//        assertTrue(point2.start());
//        state.setNextConversationPoint(point);
//        state.setNextConversationPoint(point2);
//        state.setNextConversationPoint(point);
//    }
}