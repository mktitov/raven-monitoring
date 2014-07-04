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

package org.raven.sched.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.NodeException;

/**
 *
 * @author Mikhail Titov
 */
public class ActorSystemNodeTest extends RavenCoreTestCase {
    
    private ActorSystemNode actorSystem;
    
    @Before
    public void prepare() {
        actorSystem = new ActorSystemNode();
        actorSystem.setName("actor system");
        testsNode.addAndSaveChildren(actorSystem);
    }
    
    @Test(expected = NodeException.class)
    public void getActorSystemOnStopped() throws NodeException {
        actorSystem.getActorSystem();
    }
    
    @Test
    public void getActorSystem() throws NodeException {
        assertTrue(actorSystem.start());
        assertNotNull(actorSystem.getActorSystem());
    }
}
