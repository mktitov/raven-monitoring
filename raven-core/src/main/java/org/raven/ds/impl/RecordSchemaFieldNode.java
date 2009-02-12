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

package org.raven.ds.impl;

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaNode.class)
public class RecordSchemaFieldNode extends BaseNode implements RecordSchemaField
{
    @Parameter @NotNull
    private RecordSchemaFieldType fieldType;

    @Parameter
    private String pattern;

    public RecordSchemaFieldType getFieldType()
    {
        return fieldType;
    }

    public void setFieldType(RecordSchemaFieldType fieldType)
    {
        this.fieldType = fieldType;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public <E> E getFieldExtension(Class<E> extensionType, String extensionName)
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null && childs.size()>0)
            for (Node child: childs)
                if (   Status.STARTED==child.getStatus()
                    && extensionType.isAssignableFrom(child.getClass())
                    && (extensionName==null || extensionName.equals(child.getName())))
                {
                    return (E)child;
                }

        return null;
    }
}
