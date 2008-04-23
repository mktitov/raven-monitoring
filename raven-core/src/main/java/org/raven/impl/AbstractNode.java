/*
 *  Copyright 2008 tim.
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

package org.raven.impl;

import java.util.List;
import org.raven.Node;
import org.raven.NodeAttribute;
import org.raven.NodeInitializationError;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractNode<T> implements Node<T>
{
    private String name;
    private final Class[] childNodeTypes;
    private List<Node> childrens;
    private List<NodeAttribute> nodeAttributes;
    private Class<? extends T> wrappedObjectType;
    private T nodeLogic;
    private int initializationPriority;
    private Node parentNode;

    public AbstractNode(Class[] childNodeTypes)
    {
        this.childNodeTypes = childNodeTypes;
    }

    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public Class<? extends T> getNodeLogicType()
    {
        return wrappedObjectType;
    }

    public void setNodeLogicType(Class<? extends T> wrappedObjectType)
    {
        this.wrappedObjectType = wrappedObjectType;
    }

    public Class[] getChildNodeTypes()
    {
        return childNodeTypes;
    }

    public List<Node> getChildrens()
    {
        return childrens;
    }

    public int getInitializationPriority()
    {
        return initializationPriority;
    }

    public List<NodeAttribute> getNodeAttributes()
    {
        return nodeAttributes;
    }

    public T getNodeLogic()
    {
        return nodeLogic;
    }

    public Node getParentNode()
    {
        return parentNode;
    }

    public void setParentNode(Node parentNode)
    {
        this.parentNode = parentNode;
    }

    public String getPath()
    {
        StringBuffer path = new StringBuffer(name);
        Node parent;
        while ( (parent=getParentNode()) != null )
            path.insert(0, parent.getName()+"/");
        
        return path.toString();
    }
    
    public void init() throws NodeInitializationError
    {
        if (wrappedObjectType!=null)
        {
            createWrappedObject();
        }
        //extract wrapped node parameters;
        
        //create wrapped object if it is setted
    }

    private void createWrappedObject() throws NodeInitializationError
    {
        try
        {
            nodeLogic = wrappedObjectType.newInstance();
        } catch (Exception e)
        {
            throw new NodeInitializationError(
                    String.format(
                        "Error while creating the wrapped object of the type (%s) for node (%s)"
                        , getPath(), name)
                    , e);
        }
    }
}
