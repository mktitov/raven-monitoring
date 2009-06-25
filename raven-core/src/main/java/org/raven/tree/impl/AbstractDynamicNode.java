/*
 *  Copyright 2009 Mikhail Titov.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDynamicNode extends BaseNode
{
    private long lastAccessTime = 0;
    private AtomicBoolean childrensInitializing;

    public AbstractDynamicNode()
    {
        setChildrensDynamic(true);
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        childrensInitializing = new AtomicBoolean(false);
    }

    @Override
    public Node getChildren(String name)
    {
        Node child = super.getChildren(name);
        if (   child==null && !childrensInitializing.get() && lastAccessTime==0
            && Status.STARTED.equals(getStatus()))
        {
            getChildrens();
            child = super.getChildren(name);
        }
        return child;
    }

    @Override
    public synchronized Collection<Node> getChildrens()
    {
        childrensInitializing.set(true);
        try
        {
            lastAccessTime = System.currentTimeMillis();
            Collection<Node> newNodes = doGetChildrens();
            Set<String> nodeNames = new HashSet<String>();
            if (newNodes!=null && !newNodes.isEmpty())
                for (Node newNode: newNodes)
                {
                    if (getChildren(newNode.getName())==null)
                    {
                        addAndSaveChildren(newNode);
                        if (newNode.isAutoStart())
                            newNode.start();
                    }
                    nodeNames.add(newNode.getName());
                }
            Collection<Node> childs = super.getChildrens();
            if (childs!=null && !childs.isEmpty())
            {
                childs = new ArrayList<Node>(childs);
                for (Node child: childs)
                    if (!nodeNames.contains(child.getName()))
                        removeChildren(child);
            }

            return super.getChildrens();
        }
        finally
        {
            childrensInitializing.set(false);
        }
    }

    /**
     * Removes all children nodes
     */
    public synchronized void removeChildrens()
    {
        Collection<Node> childs = super.getChildrens();
        if (childs!=null && !childs.isEmpty())
        {
            Collection<Node> detachedChilds = new ArrayList<Node>(childs);
            for (Node child: detachedChilds)
                removeChildren(child);
        }
    }

    public synchronized void removeChildrensIfExpired(long expirationInterval)
    {
        if (lastAccessTime!=0 && (System.currentTimeMillis()-expirationInterval)>lastAccessTime)
        {
            removeChildrens();
            lastAccessTime = 0;
        }
        else
        {
            Collection<Node> childs = super.getChildrens();
            if (childs!=null && !childs.isEmpty())
                for (Node child: childs)
                    if (child instanceof AbstractDynamicNode)
                        ((AbstractDynamicNode)child).removeChildrensIfExpired(expirationInterval);
        }
    }

    protected abstract Collection<Node> doGetChildrens();
}
