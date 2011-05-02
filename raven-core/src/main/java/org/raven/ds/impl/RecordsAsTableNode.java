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
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.ReferenceValuesSource;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.ReferenceValue;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={RecordFieldReferenceValuesNode.class})
public class RecordsAsTableNode extends BaseNode implements Viewable, DataSource
{
    public final static String DATA_SOURCE_ATTR = "dataSource";
    public static final String RECORDS_BINDING = "records";
    public final static String RECORD_SCHEMA_ATTR = "recordSchema";
    public final static String RECORD_BINDING = "record";
    public final static String VALUE_BINDING = "value";

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private DataSource dataSource;

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchema recordSchema;

    @Parameter
    private String fieldsOrder;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean showFieldsInDetailColumn;

    @Parameter
    private Integer detailColumnNumber;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean autoRefresh;

    @NotNull @Parameter(defaultValue="false")
    private Boolean enableDeletes;

    @NotNull @Parameter(defaultValue="true")
    private Boolean refreshViewAfterDelete;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String cellValueExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useCellValueExpression;

    private BindingSupportImpl bindingSupport;


    @Message
    private String detailColumnName;
    @Message
    private String fieldNameColumnName;
    @Message
    private String fieldValueColumnName;
    @Message
    private String detailValueViewLinkName;
    @Message
    private static String deleteMessage;
    @Message
    private static String deleteConfirmationMessage;
    @Message
    private static String deleteCompletionMessage;

    private Map<String, RecordSchemaField> fields;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public Integer getDetailColumnNumber()
    {
        return detailColumnNumber;
    }

    public void setDetailColumnNumber(Integer detailColumnNumber)
    {
        this.detailColumnNumber = detailColumnNumber;
    }

    public Boolean getShowFieldsInDetailColumn()
    {
        return showFieldsInDetailColumn;
    }

    public void setShowFieldsInDetailColumn(Boolean showFieldsInDetailColumn)
    {
        this.showFieldsInDetailColumn = showFieldsInDetailColumn;
    }

    public Boolean getAutoRefresh()
    {
        return autoRefresh;
    }

    public void setAutoRefresh(Boolean autoRefresh)
    {
        this.autoRefresh = autoRefresh;
    }

    public Boolean getEnableDeletes()
    {
        return enableDeletes;
    }

    public void setEnableDeletes(Boolean enableDeletes)
    {
        this.enableDeletes = enableDeletes;
    }

    public Boolean getRefreshViewAfterDelete() {
        return refreshViewAfterDelete;
    }

    public void setRefreshViewAfterDelete(Boolean refreshViewAfterDelete) {
        this.refreshViewAfterDelete = refreshViewAfterDelete;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public String getFieldsOrder()
    {
        return fieldsOrder;
    }

    public void setFieldsOrder(String fieldsOrder)
    {
        this.fieldsOrder = fieldsOrder;
    }

    public RecordSchema getRecordSchema()
    {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchema recordSchema)
    {
        this.recordSchema = recordSchema;
    }

    public String getCellValueExpression() {
        return cellValueExpression;
    }

    public void setCellValueExpression(String cellValueExpression) {
        this.cellValueExpression = cellValueExpression;
    }

    public Boolean getUseCellValueExpression() {
        return useCellValueExpression;
    }

    public void setUseCellValueExpression(Boolean useCellValueExpression) {
        this.useCellValueExpression = useCellValueExpression;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        if (!ObjectUtils.in(getStatus(), Status.STARTED, Status.INITIALIZED))
            return null;
        
        Map<String, NodeAttribute> attrs = NodeUtils.extractRefereshAttributes(this);
        if (attrs==null || attrs.isEmpty())
            return null;

        fields = new HashMap<String, RecordSchemaField>();
        RecordSchemaField[] schemaFields = recordSchema.getFields();
        if (schemaFields!=null)
            for (RecordSchemaField field: schemaFields)
                fields.put(field.getName(), field);
        if (!fields.isEmpty())
            for (NodeAttribute attr: attrs.values())
            {
                RecordSchemaField field = fields.get(attr.getName());
                if (field!=null) {
                    attr.setDisplayName(field.getDisplayName());
                    attr.setReferenceValuesSource(field.getReferenceValuesSource());
                }
            }

        for (RecordFieldReferenceValuesNode node:
                NodeUtils.getChildsOfType(this, RecordFieldReferenceValuesNode.class))
        {
            NodeAttribute attr = attrs.get(node.getFieldName());
            if (attr!=null)
                attr.setReferenceValuesSource(node.getReferenceValuesSource());
        }

        return attrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        if (refreshAttributes!=null)
            attrs.putAll(refreshAttributes);

        for (NodeAttribute attr: getNodeAttributes())
            if (   DATA_SOURCE_ATTR.equals(attr.getParentAttribute())
                && !attrs.containsKey(attr.getName()))
            {
                attrs.put(attr.getName(), attr);
            }

        String _fieldsOrder = fieldsOrder;
        String[] fieldsOrderArr = _fieldsOrder==null? null : _fieldsOrder.split("\\s*,\\s*");

        Map<Integer, RecordsAsTableColumnValueNode> columnValues = getColumnValues();

        Map<String, Map<Object, String>> fieldRefValues = getRecordFieldReferenceValues();

        List<Record> records = getActionsCount()>0? new ArrayList<Record>(512) : null;

        RecordAsTableDataConsumer dataConsumer = new RecordAsTableDataConsumer(
                fieldsOrderArr, detailColumnName, detailValueViewLinkName
                , fieldNameColumnName, fieldValueColumnName, detailColumnNumber, columnValues
                , deleteConfirmationMessage, deleteMessage, deleteCompletionMessage
                , getRecordActions(), records, fieldRefValues);

        dataSource.getDataImmediate(dataConsumer, new DataContextImpl(attrs));

        List<ViewableObject> vos = new ArrayList<ViewableObject>();
//        if (records!=null)
//        {
            List<ViewableObject> actions = getActions(refreshAttributes, records, dataConsumer.context);
            if (actions!=null)
                vos.addAll(actions);
//        }
        
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, dataConsumer.getTable()));
        
        return vos;
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);

        if (attribute.getName().equals(RECORD_SCHEMA_ATTR))
        {
            if (newValue==null)
                fields = null;
            else
            {
                fields = new HashMap<String, RecordSchemaField>();
                RecordSchemaField[] schemaFields = ((RecordSchema)newValue).getFields();
                if (schemaFields!=null)
                    for (RecordSchemaField field: schemaFields)
                        fields.put(field.getName(), field);
                if (fields.isEmpty())
                    fields = null;
            }
        }
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        throw new UnsupportedOperationException(
                String.format("Datasource (%s) can work only in push mode", getPath()));
    }

    public Collection<NodeAttribute> generateAttributes() 
    {
        return null;
    }

    private Map<String, Map<Object, String>> getRecordFieldReferenceValues()
    {
        Map<String, Map<Object, String>> fieldRefValues = null;
        for (RecordFieldReferenceValuesNode node: NodeUtils.getChildsOfType(
                this, RecordFieldReferenceValuesNode.class))
        {
            if (fieldRefValues==null)
                fieldRefValues = new HashMap<String, Map<Object, String>>();
            Map<Object, String> values =
                    refValuesToMap(node.getReferenceValuesSource().getReferenceValues());
            if (values!=null)
                fieldRefValues.put(node.getFieldName(), values);
        }
        return fieldRefValues;
    }

    private static Map<Object, String> refValuesToMap(List<ReferenceValue> refValues)
    {
        if (refValues!=null && !refValues.isEmpty()) {
            Map<Object, String> values = new HashMap<Object, String>();
            for (ReferenceValue value: refValues)
                values.put(value.getValue(), value.getValueAsString());
            return values;
        }
        return null;
    }

    private Map<Integer, RecordsAsTableColumnValueNode> getColumnValues()
    {
        Map<Integer, RecordsAsTableColumnValueNode> columnValues = null;
        for (RecordsAsTableColumnValueNode node:
            NodeUtils.getChildsOfType(this, RecordsAsTableColumnValueNode.class))
        {
            if (columnValues==null)
                columnValues = new HashMap<Integer, RecordsAsTableColumnValueNode>();
            columnValues.put(node.getColumnNumber()-1, node);
        }
            
        return columnValues;
    }

    private List<RecordsAsTableRecordActionNode> getRecordActions()
    {
       return NodeUtils.getChildsOfType(this, RecordsAsTableRecordActionNode.class);
    }

    private int getActionsCount()
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null && !childs.isEmpty())
        {
            int count = 0;
            for (Node child: childs)
                if (child instanceof RecordsAsTableActionNode && Status.STARTED.equals(child.getStatus()))
                    ++count;
            return count;
        }

        return 0;
    }
    
    private List<ViewableObject> getActions(
            Map<String, NodeAttribute> refreshAttributes, Collection<Record> records, DataContext context) throws Exception
    {
        Collection<Node> childs = getSortedChildrens();
        if (childs!=null && !childs.isEmpty())
        {
            List<ViewableObject> actions = null;
            for (Node child: childs)
                if (   child instanceof RecordsAsTableActionNode
                    && Status.STARTED.equals(child.getStatus()))
                {
                    if (actions==null)
                        actions = new ArrayList<ViewableObject>();
                    Map<String, Object> bindings = new HashMap<String, Object>();
                    bindings.put(RECORDS_BINDING, records);
                    bindings.put(AbstractSafeDataPipe.DATA_CONTEXT_BINDING, context);
                    actions.add(((RecordsAsTableActionNode)child).getActionViewableObject(new DataContextImpl(), bindings));
                }
            return actions;
        }
        else
            return null;
    }

    public class RecordAsTableDataConsumer implements DataConsumer
    {
        public static final String CELL_VALUE_EXPRESSION_ATTR = "cellValueExpression";
        public static final String COLUMN_NUMBER_BINDING = "columnNumber";
        private final TableImpl table;
        private final String[] fieldNames;
        private final Map<String, RecordSchemaField> fields;
        private final RecordSchema recordSchema;
        private final boolean showFieldsInDetailColumn;
        private final RecordSchemaField[] schemaFields;
        private final String detailColumnName;
        private final String fieldNameColumnName;
        private final String fieldValueColumnName;
        private final String detailValueViewLinkName;
        private final int detailColumnNumber;
        private final String deleteConfirmationMessage;
        private final String deleteMessage;
        private final String deleteCompletionMessage;
        private final Map<Integer, RecordsAsTableColumnValueNode> columnValues;
        private final List<RecordsAsTableRecordActionNode> recordActions;
        private final List<Record> records;
        private final Map<String, Map<Object, String>> fieldRefValues;
        private Map<String, Map<Object, String>> schemaFieldRefValues;
        private int actionsCount;
        private int columnsCount;
        private DataContext context;

        public RecordAsTableDataConsumer(
                String[] fieldsOrder, String detailColumnName, String detailValueViewLinkName
                , String fieldNameColumnName
                , String fieldValueColumnName
                , Integer detailColumnNumber
                , Map<Integer, RecordsAsTableColumnValueNode> columnValues
                , String deleteConfirmationMessage, String deleteMessage
                , String deleteCompletionMessage
                , List<RecordsAsTableRecordActionNode> recordActions
                , List<Record> records
                , Map<String, Map<Object, String>> fieldRefValues)
            throws Exception
        {
            fields = new HashMap<String, RecordSchemaField>();
            this.recordSchema = RecordsAsTableNode.this.recordSchema;
            this.showFieldsInDetailColumn = RecordsAsTableNode.this.showFieldsInDetailColumn;
            this.detailColumnName = detailColumnName;
            this.detailValueViewLinkName = detailValueViewLinkName;
            this.fieldNameColumnName = fieldNameColumnName;
            this.fieldValueColumnName = fieldValueColumnName;
            this.columnValues = columnValues;
            this.deleteCompletionMessage = deleteCompletionMessage;
            this.deleteConfirmationMessage = deleteConfirmationMessage;
            this.deleteMessage = deleteMessage;
            this.recordActions = recordActions;
            this.records = records;
            this.fieldRefValues = fieldRefValues;
            
            schemaFields = recordSchema.getFields();
            if (fieldsOrder!=null)
            {
                fieldNames = fieldsOrder;
                String[] sortedNames = fieldsOrder.clone();
                Arrays.sort(sortedNames);
                for (RecordSchemaField field: schemaFields)
                    if (Arrays.binarySearch(sortedNames, field.getName())>=0)
                        fields.put(field.getName(), field);
            }
            else
            {
                fieldNames = new String[schemaFields.length];
                for (int i=0; i<schemaFields.length; ++i)
                {
                    fieldNames[i] = schemaFields[i].getName();
                    fields.put(schemaFields[i].getName(), schemaFields[i]);
                }
            }

            for (RecordSchemaField field: fields.values()) {
                ReferenceValuesSource valuesSource = field.getReferenceValuesSource();
                if (valuesSource!=null) {
                    Map<Object, String> values = refValuesToMap(valuesSource.getReferenceValues());
                    if (values!=null) {
                        if (schemaFieldRefValues==null)
                            schemaFieldRefValues = new HashMap<String, Map<Object, String>>();
                        schemaFieldRefValues.put(field.getName(), values);
                    }
                }
            }

            if (enableDeletes)
                ++actionsCount;
            if (recordActions!=null)
                actionsCount += recordActions.size();
            columnsCount = actionsCount + fieldNames.length +
                    (showFieldsInDetailColumn&&detailColumnNumber==null? 1 : 0);
            String[] columnNames = new String[columnsCount];

            for (int i=0; i<fieldNames.length; ++i)
            {
                RecordSchemaField field = fields.get(fieldNames[i]);
                if (field==null)
                {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error("Can't find field ({}) in the record schema ({})"
                                , fieldNames[i], recordSchema.getName());
                    throw new Exception(String.format(
                            "Can't find field (%s) in the record schema (%s)"
                            , fieldNames[i], recordSchema.getName()));
                }
                String displayName = fields.get(fieldNames[i]).getDisplayName();
                columnNames[i+actionsCount] = displayName==null? fieldNames[i] : displayName;
            }

            if (showFieldsInDetailColumn&&detailColumnNumber==null)
                columnNames[columnNames.length-1] = detailColumnName;

            this.detailColumnNumber = detailColumnNumber==null? -1 : detailColumnNumber-1;

            table = new TableImpl(columnNames);
        }

        public TableImpl getTable()
        {
            return table;
        }

        public void setData(DataSource dataSource, Object data, DataContext context)
        {
            this.context = context;
            if (data==null)
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "All records recieved from data source (%s)", dataSource.getPath()));
                return;
            }

            if (!(data instanceof Record))
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format(
                            "Invalid data type recieved from (%s) data source. " +
                            "The valid type is (%s)"
                            , dataSource.getPath(), Record.class.getName()));
                return;
            }

            Record record = (Record) data;

            if (!recordSchema.equals(record.getSchema()))
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format(
                            "Invalid record schema recived from (%s)", dataSource.getPath()));
                return;
            }

            if (records!=null)
                records.add(record);
            
//            int len = fieldNames.length + (showFieldsInDetailColumn&&detailColumnNumber<0? 1 : 0);
//            len += actionsCount;
            Object[] row = new Object[columnsCount];
            try
            {
                if (actionsCount>0)
                {
                    int pos=0;
                    if (enableDeletes)
                        row[pos++] = new DeleteRecordAction(
                                deleteConfirmationMessage, deleteMessage, deleteCompletionMessage
                                , record, refreshViewAfterDelete);
                    if (recordActions!=null)
                        for (RecordsAsTableRecordActionNode actionNode: recordActions)
                        {
                            Map<String, Object> bindings = new HashMap<String, Object>();
                            bindings.put(RECORD_BINDING, record);
                            bindings.put(AbstractSafeDataPipe.DATA_CONTEXT_BINDING, context);
                            try {
                                row[pos++] = actionNode.getActionViewableObject(new DataContextImpl(), bindings);
                            } catch (Exception ex) {
                            }
                        }
                }
                boolean _useCellValuesExpression = useCellValueExpression;
                for (int i=0; i<fieldNames.length; ++i)
                    try
                    {
                        Object value = record.getValue(fieldNames[i]);

                        if (schemaFieldRefValues!=null) {
                            Map<Object, String> refValues = schemaFieldRefValues.get(fieldNames[i]);
                            if (refValues!=null)
                                value = refValues.get(value);
                        }

                        if (fieldRefValues!=null) {
                            Map<Object, String> refValues = fieldRefValues.get(fieldNames[i]);
                            if (refValues!=null)
                                value = refValues.get(value);
                        }

                        if (_useCellValuesExpression){
                            bindingSupport.put(VALUE_BINDING, value);
                            bindingSupport.put(RECORD_BINDING, record);
                            bindingSupport.put(COLUMN_NUMBER_BINDING, i+1);
                            bindingSupport.put(AbstractSafeDataPipe.DATA_CONTEXT_BINDING, context);
                            try{
                                value = getNodeAttribute(CELL_VALUE_EXPRESSION_ATTR).getRealValue();
                            }finally{
                                bindingSupport.reset();
                            }
                        }

                        RecordsAsTableColumnValueNode columnValue =
                                columnValues==null? null : columnValues.get(i);
                        if (columnValue!=null)
                        {
                            columnValue = columnValues.get(i);
                            columnValue.addBinding(VALUE_BINDING, value);
                            columnValue.addBinding(RECORD_BINDING, record);
                            columnValue.addBinding(AbstractSafeDataPipe.DATA_CONTEXT_BINDING, context);
                            value = columnValue.getNodeAttribute(
                                    RecordsAsTableColumnValueNode.COLUMN_VALUE_ATTR).getRealValue();
                        }

                        value = converter.convert(
                                String.class, value
                                , fields.get(fieldNames[i]).getPattern());
                        if (showFieldsInDetailColumn && detailColumnNumber==i)
                            row[i+actionsCount] = createDetailObject((String)value, record);
                        else
                            row[i+actionsCount] = value;
                    }
                    catch (Exception e)
                    {
                        if (isLogLevelEnabled(LogLevel.ERROR))
                            error("Error adding record value, recieved from (%s), to table", e);
                    }
            }
            finally
            {
                if (columnValues!=null)
                    for (RecordsAsTableColumnValueNode columnValue: columnValues.values())
                        columnValue.resetBindings();
            }

            if (showFieldsInDetailColumn && detailColumnNumber<0)
                row[row.length-1] = createDetailObject(detailValueViewLinkName, record);

            table.addRow(row);
        }

        private ViewableObject createDetailObject(String displayValue, Record record)
        {
            TableImpl detailTable = new TableImpl(
                    new String[]{fieldNameColumnName, fieldValueColumnName});
            for (RecordSchemaField field: schemaFields)
            {
                try
                {
                    Object val = record.getValue(field.getName());
                    val = converter.convert(String.class, val, field.getPattern());
                    detailTable.addRow(new Object[]{field.getName(), val});
                }
                catch (Exception e)
                {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error(String.format(
                                "Error adding field (%s) to detail table", field.getName())
                            , e);
                }
            }
            ViewableObject detailObject = new ViewableObjectImpl(
                    Viewable.RAVEN_TABLE_MIMETYPE, detailTable, displayValue);
            
            return detailObject;
        }

        public Object refereshData(Collection<NodeAttribute> sessionAttributes)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getPath()
        {
            return RecordsAsTableNode.this.getPath();
        }
    }

    public class DeleteRecordAction extends AbstractActionViewableObject
    {
        private final Record record;
        private final String deleteCompletionMessage;

        public DeleteRecordAction(
                String confirmationMessage, String displayMessage, String deleteCompletionMessage
                , Record record, boolean refreshViewAfterDelete)
        {
            super(confirmationMessage, displayMessage, RecordsAsTableNode.this
                    , refreshViewAfterDelete);
            this.deleteCompletionMessage = deleteCompletionMessage;
            this.record = record;
        }

        @Override
        public String executeAction() throws Exception
        {
            record.setTag(Record.DELETE_TAG, null);
            DataContext context = new DataContextImpl();
            DataSourceHelper.sendDataToConsumers(RecordsAsTableNode.this, record, context);
            DataSourceHelper.sendDataToConsumers(RecordsAsTableNode.this, null, context);
            return deleteCompletionMessage;
        }
    }
}
