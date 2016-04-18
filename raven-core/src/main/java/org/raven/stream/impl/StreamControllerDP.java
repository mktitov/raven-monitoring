/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.stream.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.stream.Streams;

/**
 *
 * @author Mikhail Titov
 */
public class StreamControllerDP extends AbstractDataProcessorLogic {
    private List<DataProcessorFacade> sources;
    private DataProcessorFacade openSource;

    @Override
    public void postInit() {
        become(new ConfigureStage());
    }

    @Override
    public Object processData(Object dataPackage) throws Exception {
        return UNHANDLED;
    }
    
    private class ConfigureStage extends Behaviour {
        private final Map<Streams.StreamElement, DataProcessorFacade> cache = new HashMap<>();
        private long elemSeq = 0;

        public ConfigureStage() {
            super("Configuring");
        }

        @Override
        public Object processData(Object message) throws Exception {
            if (message instanceof Streams.Consumer) {
                
            }
            return UNHANDLED;
        }
        
        private DataProcessorFacade materialize(Streams.StreamElement streamElem) {
            DataProcessorFacade facade = cache.get(streamElem);
            if (facade==null) {                
                DataProcessor dp = streamElem.materialize();
                String name = streamElem.getName()!=null? streamElem.getName() : ""; //Как формировать имена?
                DataProcessorFacadeConfig elemConfig = getContext().createChild(streamElem.getName(), dp);
                elemConfig.withQueue(null)
                DataProcessorFacade elem = getContext().addChild(getContext().createChild(streamElem.getName(), dp));
            }
            
        }
    }
}
