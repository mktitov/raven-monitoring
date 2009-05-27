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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.log.LogLevel;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordGeneratorPipeNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode recordSchema;

    private ThreadLocal<Record> recordStore;

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        recordStore = new ThreadLocal<Record>();
    }

    @Override
    public void setData(DataSource dataSource, Object data)
    {
        try
        {
            Record record = recordSchema.createRecord();
            recordStore.set(record);
            bindingSupport.put("record", record);
        } 
        catch (RecordException ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "Error creating record of schema (%s)", recordSchema.getName())
                    , ex);
        }
        super.setData(dataSource, data);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        try
        {
            sendDataToConsumers(recordStore.get());
        }
        finally
        {
            recordStore.remove();
        }
    }
}
