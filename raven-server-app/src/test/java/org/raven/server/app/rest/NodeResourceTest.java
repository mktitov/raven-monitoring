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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

    @Test()
    public void createNode_nullParentTest() throws Exception
    {
        Response resp = res.createNode(null, "test", ContainerNode.class.getName());
        checkResponse(resp, Response.Status.BAD_REQUEST, "Null or empty parent");
    }

    @Test()
    public void createNode_nullNameTest() throws Exception
    {
        Response resp = res.createNode("/", null, ContainerNode.class.getName());
        checkResponse(resp, Response.Status.BAD_REQUEST, "Null or empty name");
    }

    @Test()
    public void createNode_nullTypeTest() throws Exception
    {
        Response resp = res.createNode("/", "test", null);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Null or empty type");
    }

    @Test()
    public void createNode_invlidParentTest() throws Exception
    {
        Response resp = res.createNode("/dsds", "test", ContainerNode.class.getName());
        checkResponse(resp, Response.Status.BAD_REQUEST, null);
    }

    @Test()
    public void createNode_nameAlreadyExistsTest() throws Exception
    {
        Response resp = res.createNode("/", "System", ContainerNode.class.getName());
        checkResponse(resp, Response.Status.BAD_REQUEST, null);
    }

    @Test()
    public void createNode_invalidTypeTest() throws Exception
    {
        Response resp = res.createNode("/", "test", BaseNode.class.getName());
        checkResponse(resp, Response.Status.BAD_REQUEST, null);
    }

    @Test()
    public void createNode_notEnoughRightsTest() throws Exception
    {
        TestUserContextService.userContext = null;
        Response resp = res.createNode("/", "test", ContainerNode.class.getName());
        checkResponse(resp, Response.Status.BAD_REQUEST, null);
    }

    @Test
    public void createNodeTest() throws Exception
    {
        Response resp = res.createNode("/", "test", ContainerNode.class.getName());

        Node node = tree.getRootNode().getChildren("test");
        assertNotNull(node);
        assertTrue(node instanceof ContainerNode);
        checkResponse(resp, Response.Status.OK, node.getPath());
    }

    @Test
    public void deleteNodes_deleteRootTest() throws Exception
    {
        Response resp = res.deleteNodes(Arrays.asList(tree.getRootNode().getPath()));
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        assertNotNull(tree.getNode("/"));
    }

    @Test
    public void deleteNodesTest() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addAndSaveChildren(node2);

        assertNotNull(tree.getRootNode().getChildren("node1"));
        assertNotNull(tree.getRootNode().getChildren("node2"));

        res.deleteNodes(Arrays.asList(node1.getPath(), node2.getPath()));
        assertNull(tree.getRootNode().getChildren("node1"));
        assertNull(tree.getRootNode().getChildren("node2"));
    }

    @Test
    public void deleteNodes_notEnoughRights() throws Exception
    {
        TestUserContextService.userContext = null;

        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        res.deleteNodes(Arrays.asList(node.getPath()));
        assertNotNull(tree.getRootNode().getChildren("node"));
    }

    @Test
    public void moveNodes_nullNodes() throws Exception
    {
        Response resp = res.moveNodes(null, null, 0);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Nothing to move");
    }

    @Test
    public void moveNodes_emptyNodes() throws Exception
    {
        Response resp = res.moveNodes(null, Collections.EMPTY_LIST, 0);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Nothing to move");
    }

    @Test
    public void moveNodes_notEnoughRightsInDestintaion() throws Exception
    {
        TestUserContextService.userContext = null;
        Response resp = res.moveNodes("/", Arrays.asList("/System/"), 0);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Not enough rights to move nodes to the (/) node");
    }

    @Test
    public void moveNodes_rootNode() throws Exception
    {
        Response resp = res.moveNodes("/", Arrays.asList(("/")), 0);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Can not move root node");
    }

    private static void checkResponse(Response response, Response.Status status, String message)
    {
        assertNotNull(response);
        assertEquals(status.getStatusCode(), response.getStatus());
        if (message!=null)
            assertEquals(message, response.getEntity());
    }
}