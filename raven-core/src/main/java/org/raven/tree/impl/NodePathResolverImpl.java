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
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.tree.Node;
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
    private final Tree tree;

    public NodePathResolverImpl(Tree tree)
    {
        this.tree = tree;
    }

    public PathInfo resolvePath(String path, Node currentNode) throws InvalidPathException
    {
        List<PathElement> pathElements = new ArrayList<PathElement>();
        if (path.charAt(0)==Node.NODE_SEPARATOR)
        {
            currentNode = tree.getRootNode();
            pathElements.add(new PathElementImpl(null, PathElement.Type.ROOT_REFERENCE));
        } 
        else if (currentNode==null)
            throw new InvalidPathException(String.format(
                    "Invalid path (%s). " +
                    "Parameter currentNode can not be null for relative path"
                    , path));

        StrTokenizer tokenizer = new StrTokenizer(path, Node.NODE_SEPARATOR, QUOTE);
        while (tokenizer.hasNext())
        {
            String nodeName = tokenizer.nextToken();
            if (PARENT_REFERENCE.equals(nodeName))
            {
                currentNode = currentNode.getParent();
                if (currentNode==null)
                    throw new InvalidPathException(String.format(
                            "Invalid path (%s) to the node", path));
                pathElements.add(new PathElementImpl(null, PathElement.Type.PARENT_REFERENCE));
            }
            else if (!SELF_REFERENCE.equals(nodeName))
            {
                Node nextNode = currentNode.getChildren(nodeName);                    
                if (nextNode==null)
                    throw new InvalidPathException(String.format(
                            "Invalid path (%s) to the node. " +
                            "Node (%s) does not exists in the (%s) node."
                            , path, nodeName, currentNode.getPath()));
                currentNode = nextNode;
                pathElements.add(
                        new PathElementImpl(currentNode, PathElement.Type.NODE_REFERENCE));
            } 
            else
                pathElements.add(new PathElementImpl(null, PathElement.Type.SELF_REFERENCE));
        }
        
        PathElement[] elements = new PathElement[pathElements.size()];
        elements = pathElements.toArray(elements);
        
        return new PathInfoImpl(elements, currentNode);
    }
    
}
