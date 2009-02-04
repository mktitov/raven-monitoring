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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemasNode.class)
public class RecordSchemaNode extends BaseNode implements RecordSchema
{
    public RecordSchemaField[] getFields()
    {
        Collection<Node> childs = getChildrens();
        if (childs==null || childs.size()==0)
            return null;
        List<RecordSchemaField> fields = new ArrayList<RecordSchemaField>(childs.size());
        for (Node child: childs)
            if (child.getStatus()==Status.STARTED)
                fields.add((RecordSchemaField)child);

        if (fields.size()==0)
            return null;

        RecordSchemaField[] result = new RecordSchemaField[fields.size()];
        fields.toArray(result);

        return result;
    }

    public Record createRecord()
    {
        return new RecordImpl(this);
    }

    public RecordSchemaField getField(String fieldName)
    {
        Node field = getChildren(fieldName);
        if (field==null || Status.STARTED!=field.getStatus())
            return null;
        else
            return (RecordSchemaField)field;
    }
}
