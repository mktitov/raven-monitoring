/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven;

import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.raven.conf.Configurator;
import org.raven.tree.Tree;
import org.raven.tree.store.TreeStore;

/**
 *
 * @author Mikhail Titov
 */
public class RavenCoreTestCase extends ServiceTestCase
{
    protected Tree tree;
    protected Configurator configurator;
    protected TreeStore store;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
        store = configurator.getTreeStore();
        store.removeNodes();
        
        tree = registry.getService(Tree.class);
        assertNotNull(tree);
        tree.reloadTree();
    }

    @After
    public void finalizeTest()
    {
        tree.stop(tree.getRootNode());
    }
}
