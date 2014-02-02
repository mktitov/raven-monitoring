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
package org.raven.prj;

import org.junit.Before;
import org.junit.Test;
import org.raven.auth.impl.LoginManagerNode;
import org.raven.dbcp.impl.ConnectionPoolsNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.SchemasNode;

/**
 *
 * @author Mikhail Titov
 */
public class ProjectNodeTest extends RavenCoreTestCase {
    private ProjectNode project;
    
    @Before
    public void prepare() {
        project = new ProjectNode();
        project.setName("project1");
        testsNode.addAndSaveChildren(project);
        
    }
    
    @Test
    public void initNodesTest() {
        checkNode(LoginManagerNode.NAME, false);
        checkNode(ConnectionPoolsNode.NAME, false);
        checkNode(SchemasNode.NAME, false);
        checkNode(WebInterfaceNode.NAME, false);
        checkNode(UserInterfaceNode.NAME, false);
    }
    
    @Test
    public void initNodesOnStartTest() {
        project.start();
        checkNode(LoginManagerNode.NAME, true);
        checkNode(ConnectionPoolsNode.NAME, true);
        checkNode(SchemasNode.NAME, true);
        checkNode(WebInterfaceNode.NAME, true);
        checkNode(UserInterfaceNode.NAME, true);
    }
    
    private void checkNode(String name, boolean started) {
        Node node = project.getNode(name);
        assertNotNull(node);
        assertTrue(node.isStarted()==started);
    }
}