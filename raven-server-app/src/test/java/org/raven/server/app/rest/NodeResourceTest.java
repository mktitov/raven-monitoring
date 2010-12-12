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

package org.raven.server.app.rest;

import java.net.URLEncoder;
import java.util.Collection;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.raven.auth.AuthService;
import org.raven.auth.impl.UserContextImpl;
import org.raven.server.app.RavenServerAppTestCase;
import org.raven.rest.beans.NodeBean;
import org.raven.rest.beans.NodeTypeBean;
import org.raven.server.app.TestUserContextService;
import org.raven.server.app.service.IconResolver;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class NodeResourceTest extends RavenServerAppTestCase
{
    private NodeResource res;

    @Before
    public void prepare()
    {
        TestUserContextService.userContext = new UserContextImpl(AuthService.ROOT_USER_NAME, null);
        res = new NodeResource();
    }

    @Test
    public void getChildNode_getRootNode() throws Exception
    {
        Collection<NodeBean> nodes = res.getChildNodes(null);

        assertNotNull(nodes);
        assertEquals(nodes.size(), 1);
    }

    @Test
    public void getChildNode_getRootChilds() throws Exception
    {
        Collection<NodeBean> nodes = res.getChildNodes("/");

        assertNotNull(nodes);
        assertTrue(nodes.size()>=2);
    }

    @Test
    public void getChildNodeTest2() throws Exception
    {
        Collection<NodeBean> nodes = res.getChildNodes(
                tree.getRootNode().getChildren(SystemNode.NAME).getPath());

        assertNotNull(nodes);
        assertTrue(nodes.size()>2);
    }

    @Test
    public void getIconTest() throws Exception
    {
        IconResolver iconResolver = registry.getService(IconResolver.class);
        String path = iconResolver.getPath(BaseNode.class);
        assertNotNull(path);
        byte[] image = res.getIcon(URLEncoder.encode(path, "utf-8"));
        assertNotNull(image);
    }

    @Test
    public void getChildNodeTypesTest() throws Exception
    {
        Collection<NodeTypeBean> types = res.getChildNodeTypes(null);
        assertNotNull(types);
        assertTrue(types.size()>10);

        types = res.getChildNodeTypes("Templates");
        assertNotNull(types);
        assertEquals(1, types.size());
    }

    @Test(expected=Exception.class)
    public void createNode_nullParentTest() throws Exception
    {
        res.createNode(null, "test", ContainerNode.class.getName());
    }

    @Test(expected=Exception.class)
    public void createNode_nullNameTest() throws Exception
    {
        res.createNode("/", null, ContainerNode.class.getName());
    }
    
    @Test(expected=Exception.class)
    public void createNode_nullTypeTest() throws Exception
    {
        res.createNode("/", "test", null);
    }

    @Test(expected=Exception.class)
    public void createNode_invlidParentTest() throws Exception
    {
        res.createNode("/dsds", "test", ContainerNode.class.getName());
    }

    @Test(expected=Exception.class)
    public void createNode_nameAlreadyExistsTest() throws Exception
    {
        res.createNode("/", "System", ContainerNode.class.getName());
    }

    @Test(expected=Exception.class)
    public void createNode_invalidTypeTest() throws Exception
    {
        res.createNode("/", "test", BaseNode.class.getName());
    }

    @Test(expected=Exception.class)
    public void createNode_notEnoughRightsTest() throws Exception
    {
        TestUserContextService.userContext = null;
        res.createNode("/", "test", ContainerNode.class.getName());
    }

    @Test
    public void createNodeTest() throws Exception
    {
        Response resp = res.createNode("/", "test", ContainerNode.class.getName());

        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        Node node = tree.getRootNode().getChildren("test");
        assertNotNull(node);
        assertTrue(node instanceof ContainerNode);
    }
}