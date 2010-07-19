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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.AggregateFunction;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.expr.BindingSupport;
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

    @NotNull @Parameter(defaultValue="false")
    private Boolean isRecordsSorted;

    private ThreadLocal<Map<GroupKey, Aggregation>> aggregations;

    @Override
    protected void initFields()
    {
        super.initFields();
        aggregations = new ThreadLocal<Map<GroupKey, Aggregation>>(){
            @Override
            protected Map<GroupKey, Aggregation> initialValue()
            {
                return new HashMap<GroupKey, Aggregation>();
            }
        };
    }

    public RecordSchemaNode getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema) {
        this.recordSchema = recordSchema;
    }

    public Boolean getIsRecordsSorted() {
        return isRecordsSorted;
    }

    public void setIsRecordsSorted(Boolean isRecordsSorted) {
        this.isRecordsSorted = isRecordsSorted;
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (data==null && !aggregations.get().isEmpty())
        {
            try{
                for (Aggregation agg: aggregations.get().values())
                    finishAndSendAggregation(agg, context);
                sendDataToConsumers(null, context);
                return;
            }finally {
                aggregations.remove();
            }
        }
        
        if (!(data instanceof Record))
            return;
        
        List<RecordsAggregatorGroupFieldNode> groupFields =
                new ArrayList<RecordsAggregatorGroupFieldNode>();
        List<RecordsAggregatorValueFieldNode> valueFields =
                new ArrayList<RecordsAggregatorValueFieldNode>();
        Collection<Node> childs = getChildrens();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                if (Status.STARTED.equals(child.getStatus()))
                {
                    if (child instanceof RecordsAggregatorGroupFieldNode)
                        groupFields.add((RecordsAggregatorGroupFieldNode) child);
                    else if (child instanceof RecordsAggregatorValueFieldNode)
                        valueFields.add((RecordsAggregatorValueFieldNode) child);
                }
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
            for (int i=0; i<groupFields.size(); ++i)
                groupValues[i] = groupFields.get(i).getValue((Record)data);
            GroupKey key = new GroupKey(groupValues);
            Aggregation agg = aggregations.get().get(key);
            if (agg==null)
            {
                if (isRecordsSorted && aggregations.get().size()>0){
                    finishAndSendAggregation(aggregations.get().values().iterator().next(), context);
                    aggregations.get().clear();
                }
                agg = createAggregation(groupFields, groupValues, valueFields);
                aggregations.get().put(key, agg);
            }
            aggregateValues(agg, valueFields, (Record)data);
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    private void finishAndSendAggregation(Aggregation agg, DataContext context) throws RecordException
    {
        Record rec = agg.getRecord();
        for (Map.Entry<String, AggregateFunction> entry: agg.getAggregateFunctions().entrySet())
        {
            AggregateFunction func = entry.getValue();
//            try{
                func.finishAggregation();
//            }finally{
//                bindingSupport.reset();
//            }
            rec.setValue(entry.getKey(), func.getAggregatedValue());
        }
        sendDataToConsumers(rec, context);
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    private Aggregation createAggregation(
            List<RecordsAggregatorGroupFieldNode> groupFields, Object[] groupValues
            , List<RecordsAggregatorValueFieldNode> valueFields)
        throws Exception
    {
        Record record = recordSchema.createRecord();
        for (int i=0; i<groupFields.size(); ++i)
            record.setValue(groupFields.get(i).getName(), groupValues[i]);
        Map<String, AggregateFunction> aggFunctions = new HashMap<String, AggregateFunction>();
        for (RecordsAggregatorValueFieldNode valueField: valueFields)
        {
            AggregateFunction func = null;
            switch(valueField.getAggregateFunction())
            {
                case AVG : func = new AvgAggregateFunction(); break;
                case COUNT : func = new CountAggregateFunction(); break;
                case MAX : func = new MaxAggregateFunction(); break;
                case MIN : func = new MinAggregateFunction(); break;
                case SUM : func = new SumAggregateFunction(); break;
                case CUSTOM : func = new CustomAggregateFunction(valueField); break;
            }
            if (func==null)
                throw new Exception(String.format(
                        "Undefned aggregate function (%s)", valueField.getAggregateFunction()));
            aggFunctions.put(valueField.getName(), func);
            func.startAggregation();
        }
        return new Aggregation(record, aggFunctions);
    }

    private void aggregateValues(
            Aggregation agg, List<RecordsAggregatorValueFieldNode> valueFields, Record sourceRecord)
        throws Exception
    {
        for (RecordsAggregatorValueFieldNode valueField: valueFields)
        {
            Object nextValue = valueField.getValue(sourceRecord);
            agg.aggregate(valueField.getName(), nextValue);
        }
    }

    private class CustomAggregateFunction implements AggregateFunction
    {
        private final RecordsAggregatorValueFieldNode fieldValueNode;
        private Object value;
        private int counter = 0;
        private final Map params = new HashMap();

        public CustomAggregateFunction(RecordsAggregatorValueFieldNode fieldValueNode)
        {
            this.fieldValueNode = fieldValueNode;
        }
        public void aggregate(Object nextValue)
        {
            bindingSupport.put("value", value);
            bindingSupport.put("nextValue", nextValue);
            bindingSupport.put("counter", counter);
            bindingSupport.put("params", params);
            ++counter;
            value = fieldValueNode.getAggregationExpression();
        }

        public Object getAggregatedValue()
        {
            return value;
        }

        public void finishAggregation() 
        {
            if (fieldValueNode.getUseFinishAggregationExpression())
            {
                bindingSupport.put("value", value);
                bindingSupport.put("counter", counter);
                bindingSupport.put("params", params);
                try{
                    value = fieldValueNode.getFinishAggregationExpression();
                }finally{
                    bindingSupport.remove("value");
                    bindingSupport.remove("counter");
                    bindingSupport.remove("params");
                }
            }
        }

        public void startAggregation() 
        {
            if (fieldValueNode.getUseStartAggregationExpression())
            {
                bindingSupport.put("params", params);
                value = fieldValueNode.getStartAggregationExpression();
            }
        }

        public void close() { }
    }

    private class GroupKey
    {
        private final Object[] groupValues;
        private final int hashCode;

        public GroupKey(Object[] groupValues)
        {
            this.groupValues = groupValues;
            hashCode = 53 + Arrays.hashCode(this.groupValues);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final GroupKey other = (GroupKey) obj;
            if (!Arrays.equals(this.groupValues, other.groupValues))
            {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }
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

        public Record getRecord()
        {
            return record;
        }

        public Map<String, AggregateFunction> getAggregateFunctions()
        {
            return aggFunctions;
        }

        public void aggregate(String fieldName, Object value)
        {
            aggFunctions.get(fieldName).aggregate(value);
        }
    }
}
