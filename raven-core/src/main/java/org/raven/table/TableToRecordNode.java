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

package org.raven.table;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.impl.AbstractDataPipe;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class TableToRecordNode extends AbstractDataPipe
{
    public static final String ROW_BINDING_NAME = "row";
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter
    private String tableColumnExtensionName;

    @Override
    protected void initFields()
    {
        super.initFields();
    }

    public RecordSchemaNode getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchemaNode recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getTableColumnExtensionName()
    {
        return tableColumnExtensionName;
    }

    public void setTableColumnExtensionName(String tableColumnExtensionName)
    {
        this.tableColumnExtensionName = tableColumnExtensionName;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data) throws Exception
    {
        if (!(data instanceof Table))
        {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "Invalid data type recieved from (%s). Recieved (%s) expected (%s)"
                        , dataSource.getPath()
                        , (data==null? "NULL" : data.getClass().getName())
                        , Table.class.getName()));
            return;
        }

        Table table = (Table) data;
        Map<Integer, FieldInfo> fieldCols = new HashMap<Integer, FieldInfo>();
        RecordSchema _recordSchema = recordSchema;
        RecordSchemaField[] fields = _recordSchema.getFields();
        String _tableColumnExtensionName = tableColumnExtensionName;
        if (fields!=null)
            for (RecordSchemaField field: fields)
            {
                TableColumnRecordFieldExtension colExt =
                        field.getFieldExtension(
                            TableColumnRecordFieldExtension.class, _tableColumnExtensionName);
                if (colExt!=null)
                    fieldCols.put(colExt.getColumnNumber(), new FieldInfo(field, colExt));
            }
        for (Iterator<Object[]> it = table.getRowIterator(); it.hasNext();)
        {
            Object[] row = it.next();
            Bindings bindings = new SimpleBindings();
            bindings.put(ROW_BINDING_NAME ,row);
            Record record = _recordSchema.createRecord();
            for (int i=0; i<row.length; ++i)
            {
                FieldInfo fieldInfo = fieldCols.get(i);
                if (fieldInfo!=null)
                {
                    Object val = fieldInfo.getColumnExtension().prepareValue(row[i], bindings);
                    record.setValue(fieldInfo.getField().getName(), val);
                }
            }
            sendDataToConsumers(record);
            sendDataToConsumers(null);
        }

    }

    private class FieldInfo
    {
        private final RecordSchemaField field;
        private final TableColumnRecordFieldExtension columnExtension;

        public FieldInfo(RecordSchemaField field, TableColumnRecordFieldExtension columnExtension)
        {
            this.field = field;
            this.columnExtension = columnExtension;
        }

        public TableColumnRecordFieldExtension getColumnExtension()
        {
            return columnExtension;
        }

        public RecordSchemaField getField()
        {
            return field;
        }
    }
}
