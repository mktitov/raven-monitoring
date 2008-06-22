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
import org.raven.tree.PathElement;
import org.raven.tree.PathInfo;

/**
 *
 * @author Mikhail Titov
 */
public class PathInfoImpl implements PathInfo
{
    private final PathElement[] pathElements;
    private final Node node;

    public PathInfoImpl(PathElement[] pathElements, Node node)
    {
        this.pathElements = pathElements;
        this.node = node;
    }

    public PathElement[] getPathElements()
    {
        return pathElements;
    }

    public Node getNode()
    {
        return node;
    }

}
