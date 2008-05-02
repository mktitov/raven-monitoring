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

package org.raven.tree.impl;

import org.apache.tapestry.ioc.RegistryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.ServiceTestCase;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.NodeNotFoundError;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class TreeServiceTest extends ServiceTestCase
{
    private static boolean checkTreeExecuted = false;
    private Tree tree;
    private Configurator configurator;
    
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest()
    {
        tree = registry.getService(Tree.class);
        assertNotNull(tree);
        
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
    }
    
    @Test()
    public void initTree1()
    {
        checkTree();
    }

    @Test()
    public void initTree2()
    {
        checkTree();
    }
    
    @Test()
    public void remove()
    {
        Node root = tree.getRootNode();
        Object rootId = configurator.getObjectId(root);
        assertNotNull(root);
        
        Node systemNode = root.getChildren(SystemNode.NAME);
        Object systemNodeId = configurator.getObjectId(systemNode);
        assertNotNull(systemNode);
        
        Node dsNode = systemNode.getChildren(DataSourcesNode.NAME);
        Object dsNodeId = configurator.getObjectId(dsNode);
        assertNotNull(dsNode);
        
        tree.remove(root);
        assertNull(configurator.getObjectById(root));
        assertNull(configurator.getObjectById(systemNode));
        assertNull(configurator.getObjectById(dsNode));
    }

    private void checkTree() throws NodeNotFoundError
    {
        if (!checkTreeExecuted)
        {
            checkTreeExecuted = true;
            configurator.deleteAll(BaseNode.class);
        }
        assertNotNull(tree.getRootNode());

        Node systemNode = tree.getNode(SystemNode.NAME);
        assertNotNull(systemNode);
    }
}
