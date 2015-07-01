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
import java.util.LinkedList;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldCodec;
import org.raven.ds.impl.AbstractDataPipe;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.ds.impl.RecordSchemaNode;
import org.raven.ds.impl.RecordSchemaValueTypeHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
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
    public static final String RECORD_BINDING = "record";
    public static final String ROW_BINDING = "row";
    public static final String CONFIGURE_RECORD_EXPRESSION_ATTR = "configureRecordExpression";
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchemaNode recordSchema;

    @Parameter
    private String tableColumnExtensionName;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Record configureRecordExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useConfigureRecordExpression;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();

        bindingSupport = new BindingSupportImpl();
    }

    public Record getConfigureRecordExpression() {
        return configureRecordExpression;
    }

    public void setConfigureRecordExpression(Record configureRecordExpression) {
        this.configureRecordExpression = configureRecordExpression;
    }

    public Boolean getUseConfigureRecordExpression() {
        return useConfigureRecordExpression;
    }

    public void setUseConfigureRecordExpression(Boolean useConfigureRecordExpression) {
        this.useConfigureRecordExpression = useConfigureRecordExpression;
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
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (!(data instanceof Table)) {
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "Invalid data type recieved from (%s). Recieved (%s) expected (%s)"
                        , dataSource.getPath()
                        , (data==null? "NULL" : data.getClass().getName())
                        , Table.class.getName()));
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        }

        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Trying to convert table to records");

        Table table = (Table) data;
        Map<Integer, Collection<FieldInfo>> fieldCols =
                new HashMap<Integer, Collection<FieldInfo>>();
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
                    addFieldInfo(fieldCols, field, colExt);
            }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format(
                    "Found (%d) fields in record schema (%s) with (%s) extension"
                    , fieldCols.size()
                    , _recordSchema.getName()
                    , TableColumnRecordFieldExtension.class.getSimpleName()));
        int recCount = 0;
        for (Iterator<Object[]> it = table.getRowIterator(); it.hasNext();)
        {
            Object[] row = it.next();
            Bindings bindings = new SimpleBindings();
            bindings.put(ROW_BINDING ,row);
            Record record = _recordSchema.createRecord();
            for (int i=0; i<row.length; ++i) {
                Collection<FieldInfo> fieldInfos = fieldCols.get(i);
                if (fieldInfos!=null)
                    for (FieldInfo fieldInfo: fieldInfos) {
                        Object val = fieldInfo.decode(row[i], bindings);
                        record.setValue(fieldInfo.getField().getName(), val);
                    }
            }
            if (useConfigureRecordExpression)
            {
                bindingSupport.put(RECORD_BINDING, record);
                bindingSupport.put(ROW_BINDING, row);
                try{
                    record = configureRecordExpression;
                }finally {
                    bindingSupport.reset();
                }
            }
            sendDataToConsumers(record, context);
            ++recCount;
        }
        sendDataToConsumers(null, context);
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("(%d) records sended to consumers", recCount));
    }

    private void addFieldInfo(
            Map<Integer, Collection<FieldInfo>> fieldCols
            , RecordSchemaField field
            , TableColumnRecordFieldExtension colExt)
    {
        int colNum = colExt.getColumnNumber();
        Collection<FieldInfo> fieldInfos = fieldCols.get(colNum);
        if (fieldInfos==null)
        {
            fieldInfos = new LinkedList<FieldInfo>();
            fieldCols.put(colNum, fieldInfos);
        }
        fieldInfos.add(new FieldInfo(field, colExt));
    }

    private class FieldInfo {
        private final RecordSchemaField field;
        private final TableColumnRecordFieldExtension columnExtension;
        private final RecordSchemaFieldCodec codec;

        public FieldInfo(RecordSchemaField field, TableColumnRecordFieldExtension columnExtension) {
            this.field = field;
            this.columnExtension = columnExtension;
            this.codec = columnExtension.getCodec();
        }

//        public TableColumnRecordFieldExtension getColumnExtension() {
//            return columnExtension;
//        }
        
        public Object decode(Object val, Bindings bindings) {
            return codec==null? columnExtension.prepareValue(val, bindings) : codec.decode(val, bindings);
        }

        public RecordSchemaField getField() {
            return field;
        }
    }
}
