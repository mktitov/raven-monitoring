/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.tree.impl;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.sched.ActorSystemProvider;
import org.raven.sched.impl.ActorSystemNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeException;
import org.raven.tree.NodeWithBehavior;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNodeWithBehaviorTest extends RavenCoreTestCase {
    private ActorSystemNode actorSystemProvider;
    private TestNodeWithBehavior node;
    
    @Before
    public void prepare() {
        actorSystemProvider = new ActorSystemNode();
        actorSystemProvider.setName("actorSystem");
        testsNode.addAndSaveChildren(actorSystemProvider);
        assertTrue(actorSystemProvider.start());
        
        node = new TestNodeWithBehavior();
        node.setName("behavior node");
        testsNode.addAndSaveChildren(node);
        node.setActorSystemProvider(actorSystemProvider);
        assertTrue(node.start());
    }
    
    @Test
    public void behaviorUnavailableTest() throws NodeException {
        Props props = Props.create(RequesterActor.class);
        TestActorRef<RequesterActor> requester = TestActorRef.create(
                actorSystemProvider.getActorSystem(), props, "requester");
        node.requestBehavior(requester);
        assertSame(BaseNodeWithBehavior.BEHAVIOR_UNAVAILABLE, requester.underlyingActor().behaviorUnavailable);
    }
    
    @Test
    public void behaviorTest() throws Exception {
        ActorRef behavior = actorSystemProvider.getActorSystem().actorOf(Props.create(BehaviorActor.class));
        node.setBehavior(behavior);
        Props props = Props.create(RequesterActor.class);
        TestActorRef<RequesterActor> requester = TestActorRef.create(
                actorSystemProvider.getActorSystem(), props, "requester");
        node.requestBehavior(requester);
        assertNotNull(requester.underlyingActor().behavior);
        assertSame(behavior, requester.underlyingActor().behavior.getBehavior());
        behavior.tell("test", requester);
        Future res = Patterns.ask(behavior, "test", 2000);
        Object resp = res.result(Duration.create(2000, TimeUnit.MILLISECONDS), null);
        assertEquals("ok", requester.underlyingActor().behaviorMessage);
    }
    
    public static class BehaviorActor extends UntypedActor {

        public BehaviorActor() throws InterruptedException {
            Thread.sleep(1500);
        }
        
        @Override
        public void onReceive(Object message) throws Exception {
            if ("test".equals(message)) 
                sender().tell("ok", self());
        }
    }
    
    public static class RequesterActor extends UntypedActor {
        private NodeWithBehavior.Behavior behavior;
        private NodeWithBehavior.BehaviorUnavailable behaviorUnavailable;
        private String behaviorMessage;

        @Override
        public void onReceive(Object message) throws Exception {
            if (message instanceof NodeWithBehavior.Behavior)
                behavior = (NodeWithBehavior.Behavior) message;
            else if (message instanceof NodeWithBehavior.BehaviorUnavailable)
                behaviorUnavailable = (NodeWithBehavior.BehaviorUnavailable) message;
            else if ("ok".equals(message)) 
                behaviorMessage = (String) message;
        }
    }
}
