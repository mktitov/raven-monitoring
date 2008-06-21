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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.NodeReferenceValueHandlerException;
import org.raven.tree.Tree;
import org.weda.internal.annotations.Service;
import org.weda.beans.ObjectUtils;
/**
 * Allows to reference to the {@link Node} or to the {@link NodeAttribute}.
 * The references can be absolute or relative. The format of reference expresion
 * is 
 * <pre>
 *  ref = ("node_name"|node_name|..)
 *  attr = ("attribute_name"|attribute_name)
 *  reference to the node expresion: path = ref[/path]
 *  reference to the attribute expression: path@attr
 * 
 * </pre>
 * @author Mikhail Titov
 */
public class NodeReferenceValueHandler 
        extends AbstractAttributeValueHandler implements NodeListener
{
    public final static char QUOTE = '"';
    public final static String PARENT_REFERENCE = "..";
    public final static String SELF_REFERENCE = ".";
    
    @Service
    private static Tree tree;
    
    private String data = null;
    protected Node node = null;
    private boolean addDependencyToNode = false;
    private PathElement[] pathElements = null;
            
    public NodeReferenceValueHandler(NodeAttribute attribute)
    {
        super(attribute);
    }

    public NodeReferenceValueHandler(NodeAttribute attribute, boolean addDependencyToNode)
    {
        super(attribute);
        this.addDependencyToNode = addDependencyToNode;
    }

    public void setData(String data) throws Exception
    {
        if (ObjectUtils.equals(this.data, data))
            return;
        
        Node currentNode = null;
        List<PathElement> newPathElements = null;
        if (data!=null && data.length()>0)
        {
            newPathElements = new ArrayList<PathElement>();
            if (data.charAt(0)==Node.NODE_SEPARATOR)
            {
                currentNode = tree.getRootNode();
                newPathElements.add(new PathElement(null, PathElement.Type.ROOT_REFERENCE));
            } else
                currentNode = attribute.getOwner();
            
            StrTokenizer tokenizer = new StrTokenizer(data, Node.NODE_SEPARATOR, QUOTE);
            while (tokenizer.hasNext())
            {
                String nodeName = tokenizer.nextToken();
                if (PARENT_REFERENCE.equals(nodeName))
                {
                    currentNode = currentNode.getParent();
                    if (currentNode==null)
                        throw new NodeReferenceValueHandlerException(String.format(
                                "Invalid path (%s) to the node", data));
                    newPathElements.add(new PathElement(null, PathElement.Type.PARENT_REFERENCE));
                }
                else if (!SELF_REFERENCE.equals(nodeName))
                {
                    Node nextNode = currentNode.getChildren(nodeName);                    
                    if (nextNode==null)
                        throw new NodeReferenceValueHandlerException(String.format(
                                "Invalid path (%s) to the node. " +
                                "Node (%s) does not exists in the (%s) node."
                                , data, nodeName, currentNode.getPath()));
                    currentNode = nextNode;
                    newPathElements.add(
                            new PathElement(currentNode, PathElement.Type.NODE_REFERENCE));
                } 
                else
                    newPathElements.add(new PathElement(null, PathElement.Type.SELF_REFERENCE));
            }
        }
        Node oldNode = node;
        node = currentNode;
        if (!ObjectUtils.equals(oldNode, node))
        {
            cleanupNodeReference(oldNode);
            initNodeReference(node, newPathElements);
            fireValueChangedEvent(oldNode, node);
        }
        attribute.save();
    }
    
    public String getData()
    {
        return data;
    }

    public Object handleData()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close()
    {
        cleanupNodeReference(node);
    }

    public boolean isReferenceValuesSupported()
    {
        return false;
    }

    public boolean isExpressionSupported()
    {
        return true;
    }

    public boolean isSubtreeListener()
    {
        return false;
    }

    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
    }

    public void nodeNameChanged(Node node, String oldName, String newName)
    {
        attribute.save();
    }

    public void childrenAdded(Node owner, Node children)
    {
    }

    public void childrenRemoved(Node owner, Node children)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue)
    {
    }

    public void nodeAttributeRemoved(Node node, NodeAttribute attribute)
    {
    }

    private void cleanupNodeReference(Node oldNode)
    {
        if (oldNode!=null)
        {
            if (addDependencyToNode)
                oldNode.removeDependentNode(attribute.getOwner());
            else
                oldNode.removeListener(this);
            for (PathElement pathElement: pathElements)
            {
                Node pathElementNode = pathElement.getNode();
                if (pathElementNode!=null && pathElementNode!=oldNode)
                    pathElementNode.removeListener(this);
            }
        }
    }

    private void initNodeReference(Node node, List<PathElement> newPathElements)
    {
        if (node!=null)
        {
            if (addDependencyToNode)
                node.addDependentNode(node);
            else
                node.addListener(this);
            
            pathElements = newPathElements.toArray(new PathElement[newPathElements.size()]);
            for (PathElement pathElement: pathElements)
            {
                Node pathElementNode = pathElement.getNode();
                if (pathElementNode!=null && pathElementNode!=node)
                    pathElementNode.addListener(this);
            }
        }
    }
    
    private void recalculateData()
    {
        if (pathElements==null)
            data = null;
        else
        {
            StringBuffer path = new StringBuffer();
            for (PathElement pathElement: pathElements)
                path.append(pathElement.getElement()).append(Node.NODE_SEPARATOR);
            data = path.toString();
        }
    }
    
    private static class PathElement
    {
        public enum Type {ROOT_REFERENCE, NODE_REFERENCE, SELF_REFERENCE, PARENT_REFERENCE};
        
        private final Node node;
        private final Type elementType;

        public PathElement(Node node, Type elementType)
        {
            this.node = node;
            this.elementType = elementType;
        }
        
        public String getElement()
        {
            switch(elementType)
            {
                case NODE_REFERENCE : return QUOTE+node.getName()+QUOTE;
                case PARENT_REFERENCE : return PARENT_REFERENCE;
                case ROOT_REFERENCE : return "";
                case SELF_REFERENCE : return SELF_REFERENCE;
            }
            return null;
        }
        
        public Node getNode()
        {
            return node;
        }
    }

}
