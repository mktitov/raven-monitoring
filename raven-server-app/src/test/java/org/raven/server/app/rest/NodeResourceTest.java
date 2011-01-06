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
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.raven.auth.AuthService;
import org.raven.auth.impl.UserContextImpl;
import org.raven.rest.beans.NodeAttributeBean;
import org.raven.server.app.RavenServerAppTestCase;
import org.raven.rest.beans.NodeBean;
import org.raven.rest.beans.NodeTypeBean;
import org.raven.server.app.TestUserContextService;
import org.raven.server.app.service.IconResolver;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;
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
        checkResponse(resp, Response.Status.OK, ""+node.getId());
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
        Response resp = res.moveNodes(null, null, 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Nothing to move");
    }

    @Test
    public void copyNodes_nullNodes() throws Exception
    {
        Response resp = res.moveNodes(null, null, 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Nothing to copy");
    }

    @Test
    public void moveNodes_emptyNodes() throws Exception
    {
        Response resp = res.moveNodes(null, Collections.EMPTY_LIST, 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Nothing to move");
    }

    @Test
    public void copyNodes_emptyNodes() throws Exception
    {
        Response resp = res.moveNodes(null, Collections.EMPTY_LIST, 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Nothing to copy");
    }

    @Test
    public void moveNodes_notEnoughRightsInDestintaion() throws Exception
    {
        TestUserContextService.userContext = null;
        Response resp = res.moveNodes("/", Arrays.asList("/System/"), 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Not enough rights to move nodes to the (/) node");
    }

    @Test
    public void copyNodes_notEnoughRightsInDestintaion() throws Exception
    {
        TestUserContextService.userContext = null;
        Response resp = res.moveNodes("/", Arrays.asList("/System/"), 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Not enough rights to copy nodes to the (/) node");
    }

    @Test
    public void moveNodes_rootNode() throws Exception
    {
        Response resp = res.moveNodes("/", Arrays.asList(("/")), 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Can not move root node");
    }

    @Test
    public void copyNodes_rootNode() throws Exception
    {
        Response resp = res.moveNodes("/", Arrays.asList(("/")), 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST, "Can not copy root node");
    }

    @Test
    public void moveNodes_toItself() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node1.addAndSaveChildren(node2);

        Response resp = res.moveNodes(node2.getPath(), Arrays.asList((node1.getPath())), 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST
                , String.format("Can't move node (%s) to it self (%s)", node1.getPath(), node2.getPath()));
    }

    @Test
    public void copyNodes_toItself() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node1.addAndSaveChildren(node2);

        Response resp = res.moveNodes(node2.getPath(), Arrays.asList((node1.getPath())), 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST
                , String.format("Can't copy node (%s) to it self (%s)"
                    , node1.getPath(), node2.getPath()));
    }

    @Test
    public void moveNodes_existsNodeWithSameName() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addAndSaveChildren(node2);

        ContainerNode node2_1 = new ContainerNode("node2");
        node1.addAndSaveChildren(node2_1);

        Response resp = res.moveNodes(node1.getPath(), Arrays.asList((node2.getPath())), 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST
                , String.format(
                            "Can't move node (%s) to the node (%s) because of it "
                            + "already has the node with the same name"
                            , node2.getPath(), node1.getPath()));
    }

    @Test
    public void copyNodes_existsNodeWithSameName() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addAndSaveChildren(node2);

        ContainerNode node2_1 = new ContainerNode("node2");
        node1.addAndSaveChildren(node2_1);

        Response resp = res.moveNodes(node1.getPath(), Arrays.asList((node2.getPath())), 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);
        List<Node> childs = node1.getSortedChildrens();
        checkNodeIndexes(childs, 2);
        assertEquals("node2_1", childs.get(0).getName());
        assertSame(node2_1, childs.get(1));
    }

    @Test
    public void moveNodes_existsNodeWithSameName2() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        tree.getRootNode().addAndSaveChildren(node3);

        ContainerNode node3_2 = new ContainerNode("node2");
        node3.addAndSaveChildren(node3_2);

        Response resp = res.moveNodes(
                node1.getPath()
                , Arrays.asList(node2.getPath(), node3_2.getPath())
                , 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.BAD_REQUEST
                , String.format(
                            "Can't move node (%s) to the node (%s) because of it "
                            + "already has the node with the same name"
                            , node3_2.getPath(), node1.getPath()));
    }

    @Test
    public void copyNodes_existsNodeWithSameName2() throws Exception
    {
        ContainerNode node1 = new ContainerNode("node1");
        tree.getRootNode().addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        tree.getRootNode().addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        tree.getRootNode().addAndSaveChildren(node3);

        ContainerNode node3_1 = new ContainerNode("node2");
        node3.addAndSaveChildren(node3_1);

        ContainerNode node2_1 = new ContainerNode("node2");
        node1.addAndSaveChildren(node2_1);

        Response resp = res.moveNodes(
                node1.getPath()
                , Arrays.asList(node2.getPath(), node3_1.getPath())
                , 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);
        List<Node> childs = node1.getSortedChildrens();
        checkNodeIndexes(childs, 3);
        checkNodeNames(childs, "node2_1", "node2_2", "node2");
    }

    @Test
    public void moveNodes_reposition() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        node.addAndSaveChildren(node3);

        Response resp = res.moveNodes(node.getPath(), Arrays.asList((node2.getPath())), 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        List<Node> childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 3);
        assertArrayEquals(new Object[]{node2, node1, node3}, childs.toArray());

        resp = res.moveNodes(node.getPath(), Arrays.asList(node2.getPath(), node1.getPath()), 3, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 3);
        assertArrayEquals(new Object[]{node3, node2, node1}, childs.toArray());
    }

    @Test
    public void copyNodes_toTheSameParent() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        node.addAndSaveChildren(node3);

        Response resp = res.moveNodes(node.getPath(), Arrays.asList((node2.getPath())), 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        List<Node> childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 4);
        checkNodeNames(childs, "node2_1", "node1", "node2", "node3");

        resp = res.moveNodes(node.getPath(), Arrays.asList(node2.getPath(), node1.getPath()), 4, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 6);
        checkNodeNames(childs, "node2_1", "node1", "node2", "node3", "node2_2", "node1_1");
    }

    @Test
    public void moveNodes_move() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        tree.getRootNode().addAndSaveChildren(node3);

        Response resp = res.moveNodes(node.getPath(), Arrays.asList((node3.getPath())), 0, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        List<Node> childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 3);
        assertArrayEquals(new Object[]{node3, node1, node2}, childs.toArray());
    }

    @Test
    public void copyNodes_copy() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        tree.getRootNode().addAndSaveChildren(node3);

        Response resp = res.moveNodes(node.getPath(), Arrays.asList((node3.getPath())), 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        List<Node> childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 3);
        checkNodeNames(childs, "node3", "node1", "node2");
    }

    @Test
    public void copyNodes_copy2() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        tree.getRootNode().addAndSaveChildren(node3);

        Response resp = res.moveNodes(
                node.getPath()
                , Arrays.asList(node3.getPath(), node2.getPath())
                , 0, true);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        List<Node> childs = node.getSortedChildrens();
        checkNodeIndexes(childs, 4);
        checkNodeNames(childs, "node3", "node2_1", "node1", "node2");
    }

    @Test
    public void moveNodes_moveAndRepostion() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);

        ContainerNode node1 = new ContainerNode("node1");
        node.addAndSaveChildren(node1);

        ContainerNode node2 = new ContainerNode("node2");
        node.addAndSaveChildren(node2);

        ContainerNode node3 = new ContainerNode("node3");
        tree.getRootNode().addAndSaveChildren(node3);

        Response resp = res.moveNodes(
                node.getPath(), Arrays.asList(node3.getPath(), node2.getPath()), 2, false);
        assertNotNull(resp);
        checkResponse(resp, Response.Status.OK, null);

        List<Node> childs = node.getSortedChildrens();
        assertEquals(3, childs.size());
        for (int i=0; i<childs.size(); ++i) {
            assertEquals(i+1, childs.get(i).getIndex());
        }
        assertArrayEquals(new Object[]{node1, node3, node2}, childs.toArray());
    }

    @Test
    public void startNode() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
        
        Response resp = res.startNode(node.getPath());
        checkResponse(resp, Response.Status.OK, null);
        assertEquals(Node.Status.STARTED, node.getStatus());
    }

    @Test
    public void startNode_notEnoughRights() throws Exception
    {
        TestUserContextService.userContext = null;

        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertEquals(Node.Status.INITIALIZED, node.getStatus());

        Response resp = res.startNode(node.getPath());
        checkResponse(resp, Response.Status.BAD_REQUEST, String.format(
                "Not enough rights to %s the node (%s)", "start", node.getPath()));
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }

    @Test
    public void stopNode() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        Response resp = res.stopNode(node.getPath());
        checkResponse(resp, Response.Status.OK, null);
        assertEquals(Node.Status.INITIALIZED, node.getStatus());
    }

    @Test
    public void stopNode_notEnoughRights() throws Exception
    {
        TestUserContextService.userContext = null;

        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        assertTrue(node.start());

        Response resp = res.stopNode(node.getPath());
        checkResponse(resp, Response.Status.BAD_REQUEST, String.format(
                "Not enough rights to %s the node (%s)", "stop", node.getPath()));
        assertEquals(Node.Status.STARTED, node.getStatus());
    }

    @Test
    public void getNodeAttributes() throws Exception
    {
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        NodeAttributeImpl attr = new NodeAttributeImpl("attr", String.class, "test", "desc");
        attr.setOwner(node);
        node.addNodeAttribute(attr);
        attr.init();
        attr.save();
        assertNotNull(node.getNodeAttribute("attr"));

        Collection<NodeAttributeBean> attrs = res.getNodeAttributes(node.getPath());
        assertNotNull(attrs);
        assertEquals(2, attrs.size());
        NodeAttributeBean logLevel = null;
        NodeAttributeBean attrBean = null;
        for (NodeAttributeBean bean: attrs)
            if (bean.name.equals("logLevel"))
                logLevel = bean;
            else if (bean.name.equals("attr"))
                attrBean = bean;
        assertNotNull(logLevel);
        assertNotNull(attrBean);
        assertTrue(logLevel.builtIn);
        assertFalse(attrBean.builtIn);
    }

    @Test()
    public void getNodeAttributes_notEnoughRights()
    {
        TestUserContextService.userContext = null;
        ContainerNode node = new ContainerNode("node");
        tree.getRootNode().addAndSaveChildren(node);
        try{
            res.getNodeAttributes(node.getPath());
            fail();
        }catch(Exception e){
            assertEquals(
                    String.format("Not enough rights for node (%s) to read attributes"
                        , node.getPath())
                    , e.getMessage());
        }
    }

    private static void checkResponse(Response response, Response.Status status, String message)
    {
        assertNotNull(response);
        assertEquals(status.getStatusCode(), response.getStatus());
        if (message!=null)
            assertEquals(message, response.getEntity());
    }

    private void checkNodeIndexes(List<Node> childs, int size) 
    {
        assertNotNull(childs);
        assertEquals(size, childs.size());
        for (int i = 0; i < childs.size(); ++i) {
            assertEquals(i + 1, childs.get(i).getIndex());
        }
    }

    private void checkNodeNames(List<Node> childs, String... names)
    {
        String[] childNames = new String[childs.size()];
        for (int i=0; i<childNames.length; ++i)
            childNames[i] = childs.get(i).getName();

        assertArrayEquals(names, childNames);
    }
}