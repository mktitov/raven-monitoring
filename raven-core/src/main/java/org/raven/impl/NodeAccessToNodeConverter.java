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

package org.raven.impl;

import org.raven.api.NodeAccess;
import org.raven.tree.Node;
import org.weda.converter.TypeConverterWorker;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAccessToNodeConverter implements TypeConverterWorker<NodeAccess, Node>
{

    public Node convert(NodeAccess value, Class realTargetType, String format) 
    {
        return value.asNode();
    }

    public Class getSourceType() 
    {
        return NodeAccess.class;
    }

    public Class getTargetType() 
    {
        return Node.class;
    }

    public boolean canConvertToTargetSuperType() 
    {
        return true;
    }
}
