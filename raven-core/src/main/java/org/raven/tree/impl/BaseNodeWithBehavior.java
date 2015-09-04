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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.annotations.Parameter;
import org.raven.dp.DataProcessorFacade;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.NodeWithBehavior;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class BaseNodeWithBehavior extends BaseNode implements NodeWithBehavior {
    public final static NodeWithBehavior.BehaviorUnavailable BEHAVIOR_UNAVAILABLE = new BehaviorUnavailableImpl();
    
    @NotNull @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    protected AtomicReference<DataProcessorFacade> behavior;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        behavior = new AtomicReference<>();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        behavior.set(createBehaviour());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        final DataProcessorFacade _behaviour = behavior.getAndSet(null);
        if (_behaviour!=null)
            _behaviour.stop();
    }

    @Override
    public DataProcessorFacade getBehavior() {
        return behavior.get();
    }

    @Override
    public void requestBehavior(DataProcessorFacade requester) {
        final DataProcessorFacade actor = behavior.get();
        requester.send(actor==null? BEHAVIOR_UNAVAILABLE : new BehaviorImpl(actor));
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }
    
    public boolean sendMessageToBehavior(Object message) {
        final DataProcessorFacade _behaviour = behavior.get();
        return _behaviour==null? false : _behaviour.send(message);
    }
    
    public <T> T askBehaviour(Object message) throws Exception {
        final DataProcessorFacade _behaviour = behavior.get();
        return _behaviour==null? null :(T) _behaviour.ask(message).get();
    }

    public <T> T askBehaviour(Object message, T orElse)  {
        final DataProcessorFacade _behaviour = behavior.get();
        return _behaviour==null? orElse :(T) _behaviour.ask(message).getOrElse(orElse);
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    protected abstract DataProcessorFacade createBehaviour();

    private static class BehaviorImpl implements NodeWithBehavior.Behavior, Serializable {
        private final DataProcessorFacade behavior;

        public BehaviorImpl(DataProcessorFacade behavior) {
            this.behavior = behavior;
        }

        public DataProcessorFacade getBehavior() {
            return behavior;
        }
    }
    
    private static class BehaviorUnavailableImpl implements NodeWithBehavior.BehaviorUnavailable, Serializable {
    }
}
