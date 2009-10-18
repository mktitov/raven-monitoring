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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.h2.api.AggregateFunction;
import org.h2.store.Record;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordsAggregatorNode extends AbstractSafeDataPipe
{
    public static final String RECORD_BINDING = "record";
    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    private ThreadLocal<Map<String, Aggregation>> aggregations;

    @Override
    protected void initFields()
    {
        super.initFields();
        aggregations = new ThreadLocal<Map<String, Aggregation>>(){
            @Override
            protected Map<String, Aggregation> initialValue()
            {
                return new HashMap<String, Aggregation>();
            }
        };
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (!(data instanceof Record))
            return;
        
        List<RecordsAggregatorGroupFieldNode> groupFields =
                new ArrayList<RecordsAggregatorGroupFieldNode>();
        List<RecordsAggregatorValueFieldNode> valueFields =
                new ArrayList<RecordsAggregatorValueFieldNode>();
        Collection<Node> childs = getChildrens();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                if (child instanceof RecordsAggregatorGroupFieldNode)
                    groupFields.add((RecordsAggregatorGroupFieldNode) child);
                else if (child instanceof RecordsAggregatorValueFieldNode)
                    valueFields.add((RecordsAggregatorValueFieldNode) child);
        if (groupFields.isEmpty())
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Can't aggregate. No group fields were defined");
            return ;
        }
        if (valueFields.isEmpty())
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn("Can't aggregate. No value fields were defined");
            return ;
        }
        bindingSupport.put(RECORD_BINDING, data);
        try
        {
            Object[] groupValues = new Object[groupFields.size()];
            StringBuilder hashBuilder = new StringBuilder();
            for (int i=0; i<groupFields.size(); ++i)
            {
                Object groupValue = groupFields.get(i).getFieldValue();
                hashBuilder.append(groupValue);
            }
            String hash = hashBuilder.toString();
            Aggregation agg = aggregations.get().get(hash);
            if (agg==null)
                agg = createAggregation(groupFields, groupValues, valueFields);
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    private Aggregation createAggregation(List<RecordsAggregatorGroupFieldNode> groupFields, Object[] groupValues, List<RecordsAggregatorValueFieldNode> valueFields)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class Aggregation
    {
        private final Record record;
        private final Map<String, AggregateFunction> aggFunctions;

        public Aggregation(Record record, Map<String, AggregateFunction> aggFunctions)
        {
            this.record = record;
            this.aggFunctions = aggFunctions;
        }

        private AggregateFunction getAggregateFunction(String fieldName)
        {
            return aggFunctions.get(fieldName);
        }
    }
}
