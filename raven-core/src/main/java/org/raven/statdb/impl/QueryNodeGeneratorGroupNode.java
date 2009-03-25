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

package org.raven.statdb.impl;

import java.util.Collection;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class QueryNodeGeneratorGroupNode extends BaseNode
{
    private QueryNodeGeneratorNode nodeGenerator;
    private boolean childsInitialized = false;
    
    @Parameter(readOnly=true)
    private String childsKeyExpression;

    public QueryNodeGeneratorGroupNode()
    {
        setStartAfterChildrens(true);
    }

    public boolean isChildsInitialized()
    {
        return childsInitialized;
    }

    public QueryNodeGeneratorNode getNodeGenerator()
    {
        return nodeGenerator;
    }

    public void setNodeGenerator(QueryNodeGeneratorNode nodeGenerator)
    {
        this.nodeGenerator = nodeGenerator;
    }

    public String getChildsKeyExpression()
    {
        return childsKeyExpression;
    }

    public void setChildsKeyExpression(String childsKeyExpression)
    {
        this.childsKeyExpression = childsKeyExpression;
    }

    @Override
    public Collection<Node> getChildrens()
    {
        initChildrens();
        return super.getChildrens();
    }

    @Override
    public Node getChildren(String name)
    {
        initChildrens();
        return super.getChildren(name);
    }

    @Override
    public int getChildrenCount()
    {
        initChildrens();
        return super.getChildrenCount();
    }

    private void initChildrens()
    {
        if (Status.STARTED==getStatus() && !childsInitialized)
        {
            nodeGenerator.addChildrensFor(this);
            childsInitialized = true;
        }
    }
}
