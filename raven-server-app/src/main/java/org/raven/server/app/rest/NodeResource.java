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

import javax.ws.rs.core.Response;
import org.weda.internal.annotations.Message;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.raven.auth.NodeAccessService;
import org.raven.auth.UserContextService;
import org.raven.auth.impl.AccessControl;
import org.raven.rest.beans.NodeBean;
import org.raven.rest.beans.NodeTypeBean;
import org.raven.server.app.service.IconResolver;
import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.RootNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.annotations.Service;
import org.weda.services.ClassDescriptorRegistry;
import static org.raven.server.app.rest.RestHelper.*;
/**
 *
 * @author Mikhail Titov
 */
@Path("/node/")
public class NodeResource
{
    private final static Logger log = LoggerFactory.getLogger(NodeResource.class);

    @Service
    private static Tree tree;
    @Service
    private static IconResolver IconResolver;
    @Service
    private static NodeAccessService nodeAccessService;
    @Service
    private static UserContextService userContextService;
    @Service
    private static ClassDescriptorRegistry classDescriptor;

    @Message
    private static String nullNodeNameMessage;
    @Message
    private static String nodeNameAlreadyExists;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/childs/")
    public Collection<NodeBean> getChildNodes(@QueryParam("path") String path)
            throws Exception
    {
        boolean pathWasNull = path==null || path.isEmpty();
        path = decodeParam(path, "/");
        if (log.isDebugEnabled())
            log.debug("Looking up for child nodes of the ({})", path);

        Node node = tree.getNode(path);
        if (log.isDebugEnabled())
            log.debug("Node found");

        List<Node> childs = pathWasNull? Arrays.asList(node) : node.getSortedChildrens();
        if (childs!=null && !childs.isEmpty()){
            Collection<NodeBean> beans = new ArrayList<NodeBean>(childs.size());
            for (Node child: childs) {
                int right = nodeAccessService.getAccessForNode(child, userContextService.getUserContext());
                if (right>=AccessControl.READ)
                    beans.add(new NodeBean(
                            child.getName(), child.getPath(), IconResolver.getPath(child.getClass()),
                            child.getChildrenCount()==0? false : true,
                            right));
            }
            return beans;
        }
        return null;
    }

    @GET
    @Produces("image/png")
    @Path("/icon/")
    public byte[] getIcon(@QueryParam("path") String path) throws Exception
    {
        return IconResolver.getIcon(decodeParam(path, null));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/child-node-types/")
    public Collection<NodeTypeBean> getChildNodeTypes(@QueryParam("path") String path) throws Exception
    {
        path = path = decodeParam(path, "/");
        Node node = tree.getNode(path);
        if (node==null) {
            if (log.isWarnEnabled())
                log.warn("Invalid path (%s)", path);
            return null;
        }

        List<Class> nodeTypes = tree.getChildNodesTypes(node);
        if (nodeTypes==null || nodeTypes.isEmpty())
            return null;
        
        List<NodeTypeBean> types = new ArrayList<NodeTypeBean>(nodeTypes.size());
        for (Class nodeType: nodeTypes)
            types.add(new NodeTypeBean(
                nodeType.getName(), classDescriptor.getClassDescriptor(nodeType).getDisplayName()
                , IconResolver.getPath(nodeType)));
        
        return types;
    }

    @GET
    @Path("/create-node/")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createNode(
            @QueryParam("parent") String parentPath
            , @QueryParam("name") String name
            , @QueryParam("type") String type)
        throws Exception
    {
        try{
            if (log.isDebugEnabled())
                log.debug(String.format("Creating new node. Parent: (%s), Name (%s), type (%s)"
                        , parentPath, name, type));

            parentPath = decodeAndCheckParam(parentPath, null, "Null or empty parent");
            name = decodeAndCheckParam(name, null, "Null or empty name");
            type = decodeAndCheckParam(type, null, "Null or empty type");

            Node parent = tree.getNode(parentPath);
            int rights = nodeAccessService.getAccessForNode(parent, userContextService.getUserContext());
            if (rights < AccessControl.WRITE)
                throw new Exception(String.format(
                        "Not enough rights to create a node in the node (%s)", parentPath));
            if (parent.getChildren(name)!=null)
                throw new Exception(String.format(nodeNameAlreadyExists, parentPath, name));

            Class nodeType = Class.forName(type);
            if (!parent.getChildNodeTypes().contains(nodeType))
                throw new Exception(String.format("Invalid node type (%s)", type));

            Node node = (Node) nodeType.newInstance();
            node.setName(name);
            parent.addAndSaveChildren(node);

            return Response.ok(node.getPath()).build();
        }
        catch(Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("delete-nodes")
    public Response deleteNodes(@FormParam("nodes") List<String> nodes)
    {
        if (log.isDebugEnabled())
            log.debug("Reveived delete request for ({}) nodes", nodes);
        try
        {
            if (nodes!=null && nodes.size()>0){
                for (String path: nodes)
                    try {
                        Node node = tree.getNode(path);
                        int rights = nodeAccessService.getAccessForNode(
                                node, userContextService.getUserContext());
                        if (rights>=AccessControl.WRITE && !(node instanceof RootNode))
                            tree.remove(node);
                    } catch (InvalidPathException e){}
            }
            return Response.ok().build();
        }
        catch(Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
