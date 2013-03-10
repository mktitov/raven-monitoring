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

import org.raven.tree.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;
import org.raven.tree.PathElement;
import org.raven.tree.PathInfo;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
public class NodePathResolverImpl implements NodePathResolver
{
    private final Tree tree = TreeImpl.INSTANCE;

    public List<String> splitToPathElements(String path) {
        return new StrTokenizer(path, Node.NODE_SEPARATOR, QUOTE).getTokenList();
    }

    public PathInfo<Node> resolvePath(String path, Node currentNode) throws InvalidPathException
    {
        List<PathElement> pathElements = new ArrayList<PathElement>();
        if (path.charAt(0)==Node.NODE_SEPARATOR) {
            currentNode = tree.getRootNode();
            pathElements.add(new PathElementImpl(null, PathElement.Type.ROOT_REFERENCE));
        } else if (currentNode==null)
            throw new InvalidPathException(String.format(
                    "Invalid path (%s). " +
                    "Parameter currentNode can not be null for relative path"
                    , path));
        for (String nodeName: splitToPathElements(path)) 
//        StrTokenizer tokenizer = new StrTokenizer(path, Node.NODE_SEPARATOR, QUOTE);
//        while (tokenizer.hasNext())
//            String nodeName = tokenizer.nextToken();
            if (PARENT_REFERENCE.equals(nodeName)) {
                currentNode = currentNode.getParent();
                if (currentNode==null)
                    throw new InvalidPathException(String.format("Invalid path (%s) to the node", path));
                pathElements.add(new PathElementImpl(null, PathElement.Type.PARENT_REFERENCE));
            } else if (!SELF_REFERENCE.equals(nodeName)) {
                Node nextNode = currentNode.getNode(nodeName);                    
                if (nextNode==null)
                    throw new InvalidPathException(String.format(
                            "Invalid path (%s) to the node. " +
                            "Node (%s) does not exists in the (%s) node."
                            , path, nodeName, currentNode.getPath()));
                currentNode = nextNode;
                pathElements.add(new PathElementImpl(currentNode, PathElement.Type.NODE_REFERENCE));
            } else
                pathElements.add(new PathElementImpl(null, PathElement.Type.SELF_REFERENCE));
        PathElement[] elements = new PathElement[pathElements.size()];
        elements = pathElements.toArray(elements);
        return new PathInfoImpl(elements, currentNode);
    }
    
//    public PathInfo<NodeAttribute> resolveAttributePath(String path, Node currentNode) 
//            throws InvalidPathException
//    {
//        int pos = path.lastIndexOf(Node.ATTRIBUTE_SEPARATOR);
//        if (pos<0)
//            throw new InvalidPathException(String.format(
//                    "Invalid path (%s) to the attribute. " +
//                    "Attribute separator symbol (%s) not found"
//                    , path, Node.ATTRIBUTE_SEPARATOR));
//        String pathToNode = path.substring(0, pos);
//        PathInfo<Node> nodePathInfo = resolvePath(pathToNode, currentNode);
//        String attributeName = pathToNode.substring(pos+1);
//        NodeAttribute attr = nodePathInfo.getReferencedObject().getNodeAttribute(attributeName);
//        if (attr==null)
//            throw new InvalidPathException(String.format(
//                    "Invalid path (%s) to the attribute. " +
//                    "Node (%s) does not contains attribute (%s)"
//                    , path, nodePathInfo.getReferencedObject().getName(), attributeName));
//        return new AttributePathInfo(nodePathInfo, attr);
//    }

    public String getAbsolutePath(Node node)
    {
        StringBuffer path = new StringBuffer();
        while (node!=null)
        {
            Node parent = node.getParent();
            if (parent!=null)
                path.insert(0, QUOTE+node.getName()+QUOTE+Node.NODE_SEPARATOR);
            else
                path.insert(0, node.getName()+Node.NODE_SEPARATOR);
            node = parent;
        }
        
        return path.toString();
    }

    public String getRelativePath(Node fromNode, Node toNode)
    {
        List<Node> fPath = new ArrayList<Node>();
        fPath.add(fromNode);
        Node parent = fromNode.getParent();
        while (parent!=null)
        {
            fPath.add(parent);
            parent = parent.getParent();
        }

        Node intersectionNode = null;
        Node curNode = toNode;
        List<Node> tPath = new ArrayList<Node>();
        while (intersectionNode==null)
        {
            tPath.add(curNode);
            for (Node node: fPath)
                if (curNode.equals(node))
                {
                    intersectionNode = curNode;
                    break;
                }
            if (intersectionNode==null)
                curNode = curNode.getParent();
        }

        StringBuilder path = new StringBuilder();
        curNode = fromNode;
        for (Node node: fPath)
            if (!node.equals(intersectionNode))
                path.append(PARENT_REFERENCE+Node.NODE_SEPARATOR);
            else
                break;

        ListIterator<Node> it = tPath.listIterator(tPath.size()-1);
        for (; it.hasPrevious();)
            path.append(QUOTE+it.previous().getName()+QUOTE+Node.NODE_SEPARATOR);

        return path.toString();
    }

    public String getAbsolutePath(NodeAttribute attribute)
    {
        return getAbsolutePath(attribute.getOwner())+Node.ATTRIBUTE_SEPARATOR+attribute.getName();
    }

    public String createPath(boolean absolute, String... nodeNames)
    {
        StringBuilder buf = absolute?
            new StringBuilder(""+Node.NODE_SEPARATOR) : new StringBuilder();
        for (String nodeName: nodeNames)
            buf.append(QUOTE+nodeName+QUOTE+Node.NODE_SEPARATOR);
        
        return buf.toString();
    }

    public boolean isPathAbsolute(String path)
    {
        return path.charAt(0)==Node.NODE_SEPARATOR;
    }
}
