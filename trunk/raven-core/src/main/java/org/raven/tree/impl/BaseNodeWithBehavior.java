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
import akka.actor.ActorSystem;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ActorSystemProvider;
import org.raven.tree.NodeException;
import org.raven.tree.NodeWithBehavior;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNodeWithBehavior extends BaseNode implements NodeWithBehavior {
    public final static BehaviorUnavailable BEHAVIOR_UNAVAILABLE = new BehaviorUnavailableImpl();
    
    @NotNull @Parameter
    private ActorSystemProvider actorSystemProvider;
    
    protected AtomicReference<ActorRef> behaviorActor;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        behaviorActor = new AtomicReference<ActorRef>();
    }

    public ActorRef getBehavior() {
        return behaviorActor.get();
    }

    public void requestBehavior(ActorRef requester) {
        final ActorRef actor = behaviorActor.get();
        if (actor==null)
            requester.tell(BEHAVIOR_UNAVAILABLE, null);
        else
            requester.tell(new BehaviorImpl(actor), actor);
    }
    
    /**
     * Returns actor system or null
     */
    protected ActorSystem getActorSystem() { 
        ActorSystemProvider _provider = actorSystemProvider;
        try {
            return _provider!=null? _provider.getActorSystem() : null;
        } catch (NodeException e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error getting actor system", e);
            return null;
        }
    }

    public ActorSystemProvider getActorSystemProvider() {
        return actorSystemProvider;
    }

    public void setActorSystemProvider(ActorSystemProvider actorSystemProvider) {
        this.actorSystemProvider = actorSystemProvider;
    }
    
    private static class BehaviorImpl implements Behavior, Serializable {
        private final ActorRef behavior;

        public BehaviorImpl(ActorRef behavior) {
            this.behavior = behavior;
        }

        public ActorRef getBehavior() {
            return behavior;
        }
    }
    
    private static class BehaviorUnavailableImpl implements BehaviorUnavailable, Serializable {
    }
}
