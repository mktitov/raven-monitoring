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

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicReference;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.sched.ActorSystemProvider;
import org.raven.tree.DataFile;
import org.raven.tree.NodeException;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataFileValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = ActorSystemsNode.class)
public class ActorSystemNode extends BaseNode implements ActorSystemProvider {
    
    @Parameter(valueHandlerType = DataFileValueHandlerFactory.TYPE) 
    private DataFile config;

    private AtomicReference<ActorSystem> actorSystem;

    @Override
    protected void initFields() {
        super.initFields();
        actorSystem = new AtomicReference<ActorSystem>();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        Reader configReader = config.getDataReader();
        String systemName = "raven-actor-system-"+getId();
        if (configReader!=null) {
            Config _config = ConfigFactory.parseReader(configReader);
            actorSystem.set(ActorSystem.create(systemName, _config));
        } else
            actorSystem.set(ActorSystem.create(systemName));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        ActorSystem _actorSystem = actorSystem.getAndSet(null);
        _actorSystem.shutdown();
    }
    
    public ActorSystem getActorSystem() throws NodeException {
        ActorSystem _actorSystem = actorSystem.get();
        if (_actorSystem!=null)
            return _actorSystem;
        else
            throw new NodeException("Actor system node not started");
    }

    public DataFile getConfig() {
        return config;
    }

    public void setConfig(DataFile config) {
        this.config = config;
    }
}
