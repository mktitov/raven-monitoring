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

package org.raven.tree;

import org.raven.dp.DataProcessorFacade;

/**
 * Node with behavior realized using actor
 * @author Mikhail Titov
 */
public interface NodeWithBehavior extends Node {
    /**
     * Returns the reference to the behavior actor
     */
    public DataProcessorFacade getBehavior();
    /**
     * Methods MUST send to the <b>requester</b> one of the messages: {@link Behavior} or {@link BehaviorUnavailable}
     * @param requester 
     */
    public void requestBehavior(DataProcessorFacade requester);
    
    public interface Behavior {
        public DataProcessorFacade getBehavior();
    }
    
    public interface BehaviorUnavailable {
    }
}
