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

package org.raven.tree.impl;

import org.raven.tree.Node;
import org.raven.tree.NodePathResolver;
import org.raven.tree.PathElement;

/**
 *
 * @author Mikhail Titov
 */
public class PathElementImpl implements PathElement
{
    private final Node node;
    private final Type elementType;

    public PathElementImpl(Node node, Type elementType)
    {
        this.node = node;
        this.elementType = elementType;
    }

    public String getElement()
    {
        switch(elementType)
        {
            case NODE_REFERENCE : return 
                    NodePathResolver.QUOTE+node.getName()+NodePathResolver.QUOTE;
            case PARENT_REFERENCE : return NodePathResolver.PARENT_REFERENCE;
            case ROOT_REFERENCE : return "";
            case SELF_REFERENCE : return NodePathResolver.SELF_REFERENCE;
        }
        return null;
    }

    public Node getNode()
    {
        return node;
    }
}
