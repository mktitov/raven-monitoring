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
import org.raven.tree.NodeAttribute;
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
public class NodeReferenceValueHandler extends AbstractAttributeValueHandler
{
    public final static char QUOTE = '"';
    public final static String PARENT_REFERENCE = "..";
    public final static String THIS_REFERENCE = ".";
    
    @Service
    private static Tree tree;
    
    private String data = null;
    private Node node = null;
    private boolean addDependencyToNode = false;
    private List<Node> listenNodes = null;
            
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
        
        if (data!=null && data.length()>0)
        {
            List<Node> newListenNodes = new ArrayList<Node>();
            Node currentNode = data.charAt(0)==Node.NODE_SEPARATOR? 
                tree.getRootNode() : attribute.getOwner();
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
                }
                else if (!THIS_REFERENCE.equals(nodeName))
                {
                    Node nextNode = currentNode.getChildren(nodeName);                    
                    if (nextNode==null)
                        throw new NodeReferenceValueHandlerException(String.format(
                                "Invalid path (%s) to the node. " +
                                "Node (%s) does not exists in the (%s) node."
                                , data, nodeName, currentNode.getPath()));
                    currentNode = nextNode;
                    newListenNodes.add(currentNode);
                } 
            }
            Node oldNode = node;
            node = currentNode;
            if (!ObjectUtils.equals(oldNode, node))
            {
                if (oldNode!=null)
                    oldNode.rem
            }
        }
    }

    public String getData()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object handleData()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void close()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isReferenceValuesSupported()
    {
        return false;
    }

    public boolean isExpressionSupported()
    {
        return true;
    }

}
