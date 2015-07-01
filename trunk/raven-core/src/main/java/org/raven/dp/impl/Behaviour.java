/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.dp.impl;

import org.raven.dp.DataProcessor;

/**
 *
 * @author Mikhail Titov
 */
public abstract class Behaviour implements DataProcessor {
    private final String behaviourName;

    public Behaviour(String behaviourName) {
        this.behaviourName = behaviourName;
    }

    @Override
    public String toString() {
        return behaviourName;
    }
    
    public Behaviour andThen(DataProcessor processor) {
        return new ComposeWith(processor);
    }
    
    private class ComposeWith extends Behaviour {
        private final DataProcessor thenBehaviour;

        public ComposeWith(DataProcessor thenBehaviour) {
            super(behaviourName);
            this.thenBehaviour = thenBehaviour;
        }

        @Override
        public Object processData(Object dataPackage) throws Exception {
            Object res;
            if ( (res=Behaviour.this.processData(dataPackage))==UNHANDLED )
                res = thenBehaviour.processData(dataPackage);
            return res;
        }
    }
}
