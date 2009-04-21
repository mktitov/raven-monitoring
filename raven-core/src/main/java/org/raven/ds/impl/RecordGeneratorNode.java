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
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.Record;
import org.raven.ds.RecordFieldValueGenerator;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordGeneratorNode extends AbstractDataSource
{
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null && !childs.isEmpty())
        {
            Record rec = recordSchema.createRecord();
            for (Node child: childs)
                if (child.getStatus().equals(Status.STARTED)
                    && child instanceof RecordFieldValueGenerator)
                {
                    RecordFieldValueGenerator fieldValue = (RecordFieldValueGenerator) child;
                    rec.setValue(fieldValue.getName(), fieldValue.getFieldValue());
                }
            dataConsumer.setData(this, rec);
            return true;
        }
        else
            throw new Exception(String.format(
                    "Error generating record of type (%s) because of " +
                    "not field value generators", recordSchema.getName()));
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }
}
