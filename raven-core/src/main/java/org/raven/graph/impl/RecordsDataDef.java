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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.raven.RavenUtils;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.graph.DataSeries;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsDataDef extends AbstractDataDef implements DataConsumer
{
    public final static int INITIAL_ARAY_SIZE = 50;

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

    private ThreadLocal<List<Record>> records;

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
    public DataSeries formData(long startTime, long endTime) throws Exception
    {
        try
        {
            RecordSchemaField timestampField = RavenUtils.getRecordSchemaField(
                    recordSchema, timestampFieldName);
            if (timestampField==null)
                throw new Exception(String.format(
                        "Field (%s) not found in the record schema (%s)"
                        , timestampFieldName, recordSchema.getPath()));
            String pattern = timestampField.getPattern();
            if (pattern==null)
                throw new Exception(String.format(
                        "The pattern attribute in the field schema (%s) of the record schema (%s) " +
                        "must have a value"
                        , timestampFieldName, recordSchema.getPath()));
            NodeAttributeImpl startTimeAttr = new NodeAttributeImpl(
                    timestampFieldName, String.class, null, null);
            dataSource.getDataImmediate(this, sessionAttributes);
        }
        catch (Exception e)
        {
            error(e.getMessage());
            throw e;
        }
    }

    public void setData(DataSource dataSource, Object data)
    {
//        records;
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
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
