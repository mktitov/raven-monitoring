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

import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.dbcp.ConnectionPool;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class DatabaseRecordReaderNode extends BaseNode
{
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter(valueHandlerType=ConnectionPoolValueHandlerFactory.TYPE)
    @NotNull()
    private ConnectionPool connectionPool;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean provideFilterAttributesToConsumers;

    private List<SelectField> selectFields;


    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        RecordSchema _recordSchema = recordSchema;
        RecordSchemaField[] fields = _recordSchema.getFields();
        if (fields==null || fields.length==0)
            throw new Exception(String.format(
                    "Record schema (%s) does not contains fields", recordSchema.getName()));
        
    }

    public ConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool)
    {
        this.connectionPool = connectionPool;
    }

    public Boolean getProvideFilterAttributesToConsumers()
    {
        return provideFilterAttributesToConsumers;
    }

    public void setProvideFilterAttributesToConsumers(Boolean provideFilterAttributesToConsumers)
    {
        this.provideFilterAttributesToConsumers = provideFilterAttributesToConsumers;
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    private class SelectField
    {
        private final String fieldName;
        private final String columnName;

        public SelectField(String fieldName, String columnName)
        {
            this.fieldName = fieldName;
            this.columnName = columnName;
        }

        public String getColumnName()
        {
            return columnName;
        }

        public String getFieldName()
        {
            return fieldName;
        }
    }

    private static class FilterField
    {
        private final FilterableRecordFieldExtension filterInfo;
        private final DatabaseRecordFieldExtension dbInfo;
        private final RecordSchemaField fieldInfo;

        public FilterField(
                FilterableRecordFieldExtension filterInfo
                , DatabaseRecordFieldExtension dbInfo, RecordSchemaField fieldInfo)
        {
            this.filterInfo = filterInfo;
            this.dbInfo = dbInfo;
            this.fieldInfo = fieldInfo;
        }
        
        public static FilterField create(RecordSchemaField field)
        {
            DatabaseRecordFieldExtension dbExt =
                    field.getFieldExtension(DatabaseRecordFieldExtension.class);
            FilterableRecordFieldExtension filterExt =
                    field.getFieldExtension(FilterableRecordFieldExtension.class);

            return dbExt==null || filterExt==null? null : new FilterField(filterExt, dbExt, field);
        }

        public DatabaseRecordFieldExtension getDbInfo()
        {
            return dbInfo;
        }

        public RecordSchemaField getFieldInfo()
        {
            return fieldInfo;
        }

        public FilterableRecordFieldExtension getFilterInfo()
        {
            return filterInfo;
        }
    }
}
