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
import org.raven.tree.NodeWithBehavior;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class BaseNodeWithBehavior extends BaseNode implements NodeWithBehavior {
    public final static BehaviorUnavailable BEHAVIOR_UNAVAILABLE = new BehaviorUnavailableImpl();
    
    @NotNull @Parameter
    private ExecutorService executor;
    
    protected AtomicReference<DataProcessorFacade> behavior;

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        behavior = new AtomicReference<>();
    }

    public DataProcessorFacade getBehavior() {
        return behavior.get();
    }

    public void requestBehavior(DataProcessorFacade requester) {
        final DataProcessorFacade actor = behavior.get();
        requester.send(actor==null? BEHAVIOR_UNAVAILABLE : new BehaviorImpl(actor));
    }
    
    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    private static class BehaviorImpl implements Behavior, Serializable {
        private final DataProcessorFacade behavior;

        public BehaviorImpl(DataProcessorFacade behavior) {
            this.behavior = behavior;
        }

        public DataProcessorFacade getBehavior() {
            return behavior;
        }
    }
    
    private static class BehaviorUnavailableImpl implements BehaviorUnavailable, Serializable {
    }
}
