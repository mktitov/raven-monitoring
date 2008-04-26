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

import org.raven.tree.Node;
import org.weda.converter.TypeConverterWorker;

/**
 * Converts {@link org.raven.tree.Node} to <code>String</code>
 * 
 * @author Mikhail Titov
 */
public class NodeToStringConverter implements TypeConverterWorker<Node, String>
{

    public String convert(Node value, String format)
    {
        return value.getPath();
    }

    public Class getSourceType()
    {
        return Node.class;
    }

    public Class getTargetType()
    {
        return String.class;
    }
    
}
