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

import org.raven.tree.InvalidPathException;
import org.raven.tree.Node;
import org.raven.tree.impl.TreeImpl;
import org.weda.converter.impl.AbstractConverter;
import org.weda.converter.TypeConverterException;

/**
 * Converts <code>String</code> {@link org.raven.tree.Node} 
 * 
 * @author Mikhail Titov
 */
public class StringToNodeConverter extends AbstractConverter<String, Node>
{
    public StringToNodeConverter()
    {
        super(true);
    }

    public Node convert(String value, Class targetType, String format)
    {
        try
        {
            return TreeImpl.INSTANCE.getNode(value);
        } catch (InvalidPathException ex)
        {
            throw new TypeConverterException(ex);
        }
    }

    public Class getSourceType()
    {
        return String.class;
    }

    public Class getTargetType()
    {
        return Node.class;
    }
}
