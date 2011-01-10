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

import org.raven.rest.beans.DescriptionBean;
import org.raven.tree.NodeAttribute;
import org.raven.auth.UserContext;
import java.util.Set;
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
import org.raven.rest.beans.NodeAttributeBean;
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
                int right = nodeAccessService.getAccessForNode(
                        child, userContextService.getUserContext());
                if (right>=AccessControl.READ)
                    beans.add(new NodeBean(
                            child.getId(), child.getName(), child.getPath()
                            , child.getClass().getName()
                            , IconResolver.getPath(child.getClass())
                            , child.getChildrenCount()==0? false : true
                            , right
                            , child.getStatus().equals(Node.Status.STARTED)? 1:0));
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
    @Path("/types/")
    public Collection<NodeTypeBean> getNodeTypes() throws Exception
    {
        List<Class> nodeTypes = tree.getNodeTypes();
        List<NodeTypeBean> types = new ArrayList<NodeTypeBean>(nodeTypes.size());
        
        for (Class nodeType: nodeTypes){
            List<Class> childTypes = tree.getChildNodesTypes(nodeType);
            List<String> childTypeNames = new ArrayList<String>(childTypes.size());
            for (Class type: childTypes)
                childTypeNames.add(type.getName());
            types.add(
                new NodeTypeBean(
                    nodeType.getName()
                    , classDescriptor.getClassDescriptor(nodeType).getDisplayName()
                    , IconResolver.getPath(nodeType)
                    , childTypeNames));
        }
        
        return types;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/through-types/")
    public Collection<String> getThroughNodeTypes() throws Exception
    {
        Set<Class> types = tree.getThroughNodesTypes();
        List<String> typeNames = new ArrayList<String>(types.size());
        for (Class type: types)
            typeNames.add(type.getName());

        return typeNames;
    }

    //TODO: remove this method
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/child-node-types/")
    public Collection<NodeTypeBean> getChildNodeTypes(@QueryParam("path") String path)
            throws Exception
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
    @Path("/create/")
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

            return Response.ok(""+node.getId()).build();
        }
        catch(Exception e)
        {
            if (log.isErrorEnabled())
                log.error("Error processing move nodes request", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("delete")
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

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("move")
    public Response moveNodes(
            @FormParam("destination") String destination
            , @FormParam("nodes") List<String> nodePaths
            , @FormParam("position") int position
            , @FormParam("copy") boolean copy)
    {
        if (log.isDebugEnabled())
            log.debug("Reveived move request. Moving nodes ({}) to the ({}) node"
                    , destination, nodePaths);
        try
        {
            String oper = copy? "copy":"move";
            //Preparing for move
            if (nodePaths==null || nodePaths.isEmpty())
                throw new Exception("Nothing to "+oper);
            Node newParent = tree.getNode(destination);
            UserContext userContext = userContextService.getUserContext();
            if (nodeAccessService.getAccessForNode(newParent, userContext) < AccessControl.TREE_EDIT)
                throw new Exception(String.format(
                        "Not enough rights to %s nodes to the (%s) node", oper, destination));
            List<Node> nodes = new ArrayList<Node>(nodePaths.size());
            List<String> nodeNames = new ArrayList<String>(nodePaths.size());
            for (String path: nodePaths){
                Node node = tree.getNode(path);
                Node parent = node.getParent();
                if (parent==null)
                    throw new Exception(String.format("Can not %s root node", oper));
                if (nodeAccessService.getAccessForNode(parent, userContext)<AccessControl.TREE_EDIT)
                    throw new Exception(String.format(
                            "Not enough rights to %s node (%s) from (%s) node", oper, path, destination));
                if (newParent.getPath().startsWith(node.getPath()))
                    throw new Exception(String.format(
                            "Can't %s node (%s) to it self (%s)", oper, path, destination));
                boolean searchForNewName=false;
                String newName = node.getName();
                int index = 1;
                do {
                    Node existingNode = newParent.getChildren(newName);
                    if (!copy) {
                        if (   (existingNode!=null || nodeNames.contains(newName))
                            && !node.equals(existingNode))
                        {
                            throw new Exception(String.format(
                                    "Can't move node (%s) to the node (%s) because of it "
                                    + "already has the node with the same name"
                                    , node.getPath(), newParent.getPath()));
                        }
                    } else {
                        if (existingNode!=null || nodeNames.contains(newName)){
                            searchForNewName = true;
                            newName = node.getName()+"_"+(index++);
                        }else
                            searchForNewName = false;
                    }
                } while (searchForNewName);
                nodes.add(node);
                nodeNames.add(newName);
            }
            
            //Calculating position
            int lastPosition = newParent.getChildrenCount();
            if (position<0 || position>lastPosition)
                position = lastPosition;

            //Saving the current child nodes position
            List<Node> childNodes = newParent.getSortedChildrens();
            List<String> childs = new ArrayList<String>();
            if (childNodes!=null && !childNodes.isEmpty())
                for (Node childNode: childNodes)
                    childs.add(childNode.getName());
                
            //Move or copy nodes
            int i=0;
            for (Node node: nodes) {
                if (copy)
                    tree.copy(node, newParent, nodeNames.get(i++), null, true, true, false);
                else if (!node.getParent().equals(newParent))
                    tree.move(node, newParent, null);
            }

            //Reposition nodes
            for (i=0; i<childs.size(); ++i)
                if ( !copy && nodeNames.contains(childs.get(i)) )
                    childs.set(i, null);

            childs.addAll(position, nodeNames);
            i=1;
            for (String child: childs){
                if (child!=null){
                    Node childNode = newParent.getChildren(child);
                    childNode.setIndex(i++);
                    childNode.save();
                }
            }
            
            return Response.ok().build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("start")
    public Response startNode(@FormParam("path") String path)
    {
        return startStopNode(path, true);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("stop")
    public Response stopNode(@FormParam("path") String path)
    {
        return startStopNode(path, false);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("attributes")
    public Collection<NodeAttributeBean> getNodeAttributes(String path) throws Exception
    {
        Node node = tree.getNode(path);

        checkRights(node, AccessControl.WRITE, "read attributes");

        Collection<NodeAttribute> attrs = node.getNodeAttributes();
        Collection<NodeAttributeBean> attrBeans = null;
        if (attrs!=null && !attrs.isEmpty()) {
            attrBeans = new ArrayList<NodeAttributeBean>(attrs.size());
            for (NodeAttribute attr: attrs)
                attrBeans.add(new NodeAttributeBean(
                    attr.getName(), attr.getDisplayName()
                    , attr.getType().getName(), attr.getDescription(), attr.getValue()
                    , attr.getParentAttribute(), attr.getValueHandlerType()
                    , attr.getParameterName()==null? false:true
                    , attr.isRequired(), attr.isExpression(), attr.isTemplateExpression()
                    , attr.isReadonly()));
        }
        return attrBeans;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("type-description")
    public DescriptionBean getTypeDescription(@QueryParam("path") String nodePath) throws Exception
    {
        Node node = tree.getNode(decodeParam(nodePath, null));

        checkRights(node, AccessControl.WRITE, "read description of the node");

        return new DescriptionBean(
                classDescriptor.getClassDescriptor(node.getClass()).getDescription());
    }
        
    private void checkRights(Node node, int minimumRights, String message) throws Exception
    {
        int rights = nodeAccessService.getAccessForNode(node, userContextService.getUserContext());
        if (rights < minimumRights)
            throw new Exception(String.format(
                    "Not enough rights for node (%s) to %s", node.getPath(), message));
    }

    private Response startStopNode(String path, boolean start)
    {
        try
        {
            String oper = start? "start" : "stop";
            Node node = tree.getNode(path);
            int rights = nodeAccessService.getAccessForNode(
                    node, userContextService.getUserContext());
            if (rights<AccessControl.CONTROL)
                throw new Exception(String.format(
                        "Not enough rights to %s the node (%s)", oper, node.getPath()));
            if (start) {
                if (!Node.Status.STARTED.equals(node.getStatus()))
                    node.start();
            } else {
                if (Node.Status.STARTED.equals(node.getStatus()))
                    node.stop();
            }
            return Response.ok().build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
