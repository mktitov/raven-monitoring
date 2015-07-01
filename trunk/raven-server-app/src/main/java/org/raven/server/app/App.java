/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.server.app;

import org.apache.tapestry5.ioc.IOCUtilities;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.raven.RavenCoreModule;
import org.raven.conf.Configurator;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class App
{
    public static final String USER_CONTEXT_SESSION_ATTR = "USER_CONTEXT";
    
    public static Registry REGISTRY = null;
    
    private final static Logger logger = LoggerFactory.getLogger(App.class);

    public static void createRegistry()
    {
        RegistryBuilder builder = new RegistryBuilder();
        IOCUtilities.addDefaultModules(builder);
        builder.add(RavenCoreModule.class, RavenServerAppModule.class, UserContextServiceModule.class);
        REGISTRY = builder.build();
        REGISTRY.performRegistryStartup();
        REGISTRY.getService(Tree.class).reloadTree();
    }

    public static void shutdownRegistry()
    {
        if (REGISTRY==null)
            return;
        Tree tree = REGISTRY.getService(Tree.class);
        tree.shutdown();
        Configurator configurator = REGISTRY.getService(Configurator.class);
        try {
            configurator.close();
        } catch (Exception ex) {
            logger.error("Error shutdowning configurator", ex);
        }
        REGISTRY.shutdown();
    }
}
