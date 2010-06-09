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

package org.raven.graph.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.script.Bindings;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.graph.DataSeries;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=GraphNode.class,
    childNodes={LineInterpolatorNode.class, SplineInterpolatorNode.class})
public class RecordsDataDef extends AbstractDataDef implements DataConsumer
{
    public final static int INITIAL_ARAY_SIZE = 50;
    public static final String RECORD_BINDING = "record";

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private DataSource dataSource;

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter @NotNull
    private String timestampFieldName;

    @Parameter @NotNull
    private String valueFieldName;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useRecordFilter;

    @Parameter
    private Boolean recordFilter;

    private ThreadLocal<List<Record>> records;
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();

        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        records = new ThreadLocal<List<Record>>()
        {
            @Override
            protected List<Record> initialValue()
            {
                return new ArrayList<Record>(INITIAL_ARAY_SIZE);
            }
        };
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();

        records = null;
    }

    @Override
    public DataSeries formData(Long startTime, Long endTime) throws Exception
    {
        try
        {
            String _timestampFieldName = timestampFieldName;
            Collection<NodeAttribute> sessionAttributes = null;
            if (startTime!=null && endTime!=null)
            {
                RecordSchemaField timestampField = RavenUtils.getRecordSchemaField(
                        recordSchema, _timestampFieldName);
                if (timestampField==null)
                    throw new Exception(String.format(
                            "Field (%s) not found in the record schema (%s)"
                            , _timestampFieldName, recordSchema.getPath()));
                String pattern = timestampField.getPattern();
                if (pattern==null)
                    throw new Exception(String.format(
                            "The pattern attribute in the field schema (%s) of the " +
                            "record schema (%s) must have a value"
                            , _timestampFieldName, recordSchema.getPath()));
                SimpleDateFormat fmt = new SimpleDateFormat(pattern);
                String tsExpr = String.format(
                        "[%s, %s]"
                        , fmt.format(new Date(startTime*1000))
                        , fmt.format(new Date(endTime*1000)));

                NodeAttribute tsAttr = new NodeAttributeImpl(
                        _timestampFieldName, String.class, tsExpr, null);
                tsAttr.init();
                sessionAttributes = Arrays.asList(tsAttr);
            }

            try
            {
                dataSource.getDataImmediate(this, new DataContextImpl(sessionAttributes));

                List<Record> recs = records.get();
                if (!recs.isEmpty())
                {
                    long[] timestamps = new long[recs.size()];
                    double[] values = new double[recs.size()];

                    boolean _useRecordFilter = useRecordFilter;

                    int i=0;
                    String _valueFieldName = valueFieldName;
                    for (Record rec: recs)
                    {
                        if (_useRecordFilter)
                        {
                            bindingSupport.put(RECORD_BINDING, rec);
                            Boolean leaveRecord = recordFilter;
                            if (leaveRecord==null || !leaveRecord)
                                continue;
                        }
                        Object tsObj = rec.getValue(_timestampFieldName);
                        Long ts = converter.convert(Long.class, tsObj, null)/1000;
                        if (ts==null)
                        {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                debug("skiping data for null timestamp");
                            continue;
                        }
                        else if (i>0 && ts<=timestamps[i-1])
                        {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                debug(String.format(
                                        "skiping data for timestamp (%d) because of timestamp " +
                                        "equals or lower prevous timestamp"
                                        , ts));
                            continue;
                        }
                        timestamps[i] = ts;
                        Object valueObj = rec.getValue(_valueFieldName);
                        values[i] = converter.convert(Double.class, valueObj, null);
                        ++i;
                    }

                    if (i==0)
                        return DataSeriesImpl.EMPTY_DATA_SERIES;
                    else if (i<recs.size())
                    {
                        timestamps = Arrays.copyOf(timestamps, i);
                        values = Arrays.copyOf(values, i);
                    }

                    return new DataSeriesImpl(timestamps, values);
                }
                else
                    return DataSeriesImpl.EMPTY_DATA_SERIES;
            }
            finally
            {
                records.remove();
            }
        }
        catch (Exception e)
        {
            error(e.getMessage());
            throw e;
        }
    }

    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        if (data instanceof Record)
            records.get().add((Record)data);
        else if (data==null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Recieved end marker in record sequence");
        }
        else
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
            error(String.format(
                    "Data type (%s) recieved from (%s) data source must be instance of (%s)"
                    , data.getClass().getName(), dataSource.getPath(), Record.class.getName()));
        }
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Boolean getRecordFilter()
    {
        return recordFilter;
    }

    public void setRecordFilter(Boolean recordFilter)
    {
        this.recordFilter = recordFilter;
    }

    public Boolean getUseRecordFilter()
    {
        return useRecordFilter;
    }

    public void setUseRecordFilter(Boolean useRecordFilter)
    {
        this.useRecordFilter = useRecordFilter;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getTimestampFieldName()
    {
        return timestampFieldName;
    }

    public void setTimestampFieldName(String timestampFieldName)
    {
        this.timestampFieldName = timestampFieldName;
    }

    public String getValueFieldName()
    {
        return valueFieldName;
    }

    public void setValueFieldName(String valueFieldName)
    {
        this.valueFieldName = valueFieldName;
    }
}
