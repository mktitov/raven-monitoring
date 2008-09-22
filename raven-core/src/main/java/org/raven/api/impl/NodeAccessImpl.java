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

package org.raven.api.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.raven.api.NodeAccess;
import org.raven.api.NodeAttributeAccess;
import org.raven.ds.DataSource;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.graph.RRDef;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAccessImpl implements NodeAccess
{
    private final Node node;
    private Map<String, NodeAccess> childs;
    private Map<String, NodeAttribute> attrs;

    public NodeAccessImpl(Node node)
    {
        this.node = node;
    }
    
    public NodeAccess getParent()
    {
        return new NodeAccessImpl(node.getParent());
    }

    public String getName()
    {
        return node.getName();
    }

    public String getPath()
    {
        return node.getPath();
    }

    public Map<String, NodeAccess> getChilds()
    {
        if (childs==null)
        {
            Collection<Node> nodeChilds = node.getChildrens();
            if (nodeChilds==null)
                childs = Collections.EMPTY_MAP;
            else {
                childs = new HashMap<String, NodeAccess>();
                for (Node nodeChild: nodeChilds)
                    childs.put(nodeChild.getName(), new NodeAccessImpl(nodeChild));
            }
        } 
        return childs;
    }

    public NodeAttributeAccess getAttr(String attributeName) 
    {
        NodeAttribute attr = node.getNodeAttribute(attributeName);
        if (attr==null)
            return null;
        return new NodeAttributeAccessImpl(attr);
    }

    public NodeAttributeAccess getAt(String attributeName) 
    {
        return getAttr(attributeName);
    }

    public Node asNode() 
    {
        return node;
    }

    public RRGraphNode findGraph()
    {
        return findRRGraphNode(node);
    }

    private RRGraphNode findRRGraphNode(Node node)
    {
        RRDataSource rrds = findRRDataSource(node);

        if (rrds==null)
            return null;
        
        Collection<Node> deps = rrds.getDependentNodes();
        if (deps!=null)
            for (Node dep: deps)
                if (dep instanceof RRDef)
                    return (RRGraphNode) dep.getEffectiveParent();

        return null;
    }

    private RRDataSource findRRDataSource(Node node)
    {
        if (!(node instanceof DataSource))
            return null;
        else if (node instanceof RRDataSource)
            return ((RRDataSource)node);
        else
        {
            Collection<Node> deps = node.getDependentNodes();
            if (deps!=null)
                for (Node dep: deps)
                {
                    RRDataSource rrds = findRRDataSource(dep);
                    if (rrds!=null)
                        return rrds;
                }
        }

        return null;
    }
}
