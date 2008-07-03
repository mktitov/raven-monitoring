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

import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeListener;
import org.raven.tree.NodePathResolver;
import org.raven.tree.PathElement;
import org.raven.tree.PathInfo;
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
    @Service
    private static NodePathResolver pathResolver;

    protected String data = null;
    protected Node node = null;
    private boolean addDependencyToNode = true;
    private PathElement[] pathElements = null;
    protected boolean expressionValid = true;
    protected boolean slaveMode = false;
            
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
        if (ObjectUtils.equals(this.data, data) && expressionValid)
            return;
        
        Node currentNode = null;
        PathElement[] newPathElements = null;
        if (data!=null && data.length()>0)
        {
            PathInfo<Node> pathInfo = pathResolver.resolvePath(data, attribute.getOwner());
            currentNode = pathInfo.getReferencedObject();
            pathElements = pathInfo.getPathElements();
        }

        String oldData = this.data;
        this.data = data;
        Node oldNode = node;
        node = currentNode;
        if (!ObjectUtils.equals(oldNode, node))
        {
            cleanupNodeReference(oldNode, null);
            initNodeReference(node, newPathElements);
            if (!slaveMode)
                fireValueChangedEvent(oldNode, node);
        }
        if (!ObjectUtils.equals(this.data, oldData) && !slaveMode)
            attribute.save();
        expressionValid = true;
    }
    
    public String getData()
    {
        return data;
    }

    public Object handleData()
    {
        return node;
    }

    public void close()
    {
        cleanupNodeReference(node, null);
    }

    public boolean isExpressionValid()
    {
        return expressionValid;
    }

    public void validateExpression() throws Exception
    {
        setData(data);
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
        recalculateData();
        attribute.save();
    }

    public void childrenAdded(Node owner, Node children)
    {
    }

    public void childrenRemoved(Node owner, Node children)
    {
    }

    public void nodeShutdowned(Node shutdownedNode)
    {
        Object oldValue = node;
        cleanupNodeReference(node, shutdownedNode);
        expressionValid = false;
        node = null;
        fireExpressionInvalidatedEvent(oldValue);
    }
    
    public void nodeAttributeNameChanged(NodeAttribute attribute, String oldName, String newName)
    {
    }

    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldRealValue, Object newRealValue)
    {
    }

    public boolean nodeAttributeRemoved(Node node, NodeAttribute attribute)
    {
        return false;
    }

    protected void cleanupNodeReference(Node oldNode, Node removedNode)
    {
        if (oldNode!=null)
        {
            if (addDependencyToNode)
                oldNode.removeDependentNode(attribute.getOwner());

            for (PathElement pathElement: pathElements)
            {
                Node pathElementNode = pathElement.getNode();
                if (pathElementNode!=null && pathElementNode!=removedNode)
                    pathElementNode.removeListener(this);
            }
        }
    }

    private void initNodeReference(Node node, PathElement[] newPathElements)
    {
        if (node!=null)
        {
            if (addDependencyToNode)
                node.addDependentNode(attribute.getOwner());
            
            for (PathElement pathElement: pathElements)
            {
                Node pathElementNode = pathElement.getNode();
                if (pathElementNode!=null)
                    pathElementNode.addListener(this);
            }
        }
    }
    
    protected void recalculateData()
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
}
