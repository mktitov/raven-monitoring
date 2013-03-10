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

package org.raven.test;

import org.raven.*;
import java.io.File;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.ContainerNode;
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
    protected ContainerNode testsNode;
    
    @Override
    protected void configureRegistry(Set<Class> builder)
    {
        builder.add(RavenCoreModule.class);
    }
    
    @Before
    public void initTest() throws Exception
    {
        File tmpDir = new File("target/tmp");
        tmpDir.mkdirs();
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());
//        FileUtils.deleteDirectory(new File("target/rrd"));
        configurator = registry.getService(Configurator.class);
        assertNotNull(configurator);
		File statdbPath = new File(configurator.getConfig().getStringProperty(
				Configurator.RRD_STAT_DATABASES_PATH, null));
		try{
			FileUtils.forceDelete(statdbPath);
		} catch (Exception e) { }
		statdbPath.mkdirs();
        store = configurator.getTreeStore();
        store.removeNodes();
        
        tree = registry.getService(Tree.class);
        assertNotNull(tree);
        tree.reloadTree();
        testsNode = new ContainerNode("tests node");
        tree.getRootNode().addAndSaveChildren(testsNode);
        assertTrue(testsNode.start());
    }

    @After
    public void finalizeTest() throws Exception
    {
        tree.stop(tree.getRootNode());
        configurator.close();
    }

    public static void assertStarted(Node node)
    {
        assertEquals(Node.Status.STARTED, node.getStatus());
    }

    public static void assertInititalized(Node node)
    {
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }
}
