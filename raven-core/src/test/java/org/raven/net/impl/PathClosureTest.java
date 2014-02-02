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

package org.raven.net.impl;

import org.junit.Before;
import org.junit.Test;
import org.raven.net.NetworkResponseService;
import org.raven.prj.impl.ProjectNode;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;

/**
 *
 * @author Mikhail Titov
 */
public class PathClosureTest extends RavenCoreTestCase {
    private NodePathResolver pathResolver;
    private Node sriRootNode;
    private PathClosure path;
    private ProjectNode project;
    private SimpleResponseBuilder builder;
    
    @Before
    public void prepare() {
        NetworkResponseService respService = registry.getService(NetworkResponseService.class);
        sriRootNode = respService.getNetworkResponseServiceNode();
        pathResolver = registry.getService(NodePathResolver.class);
        path = new PathClosure(this, "/raven", pathResolver, sriRootNode);
        
        project = new ProjectNode();
        project.setName("project");
        tree.getProjectsNode().addAndSaveChildren(project);
        
        builder = new SimpleResponseBuilder();
        builder.setName("builder");
        project.getWebInterface().addAndSaveChildren(builder);
        
    }
    
    @Test
    public void stringPathTest() {
        assertEquals("/raven/sri/test", path.doCall("sri/test"));
    }
    
    @Test
    public void nodePathTest() {
        NetworkResponseGroupNode group = new NetworkResponseGroupNode();
        group.setName("test");
        sriRootNode.addAndSaveChildren(group);
        assertEquals("/raven/sri/test", path.doCall(group));
    }
    
    @Test
    public void projectNodePathTest() {
        assertEquals("/raven/projects/project", path.doCall(project.getWebInterface()));
        assertEquals("/raven/projects/project/builder", path.doCall(builder));
    }
}
