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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordsAsTableNode extends BaseNode implements Viewable
{
    public final static String DATA_SOURCE_ATTR = "dataSource";
    public final static String RECORD_SCHEMA_ATTR = "recordSchema";

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

    @Message
    private String detailColumnName;
    @Message
    private String fieldNameColumnName;
    @Message
    private String fieldValueColumnName;
    @Message
    private String detailValueViewLinkName;

    private Map<String, RecordSchemaField> fields;

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

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        if (getStatus()!=Status.STARTED)
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
                if (field!=null)
                    attr.setDisplayName(field.getDisplayName());
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

        RecordAsTableDataConsumer dataConsumer = new RecordAsTableDataConsumer(
                fieldsOrderArr, detailColumnName, detailValueViewLinkName
                , fieldNameColumnName, fieldValueColumnName, detailColumnNumber);

        dataSource.getDataImmediate(dataConsumer, attrs.values());

        ViewableObject table = new ViewableObjectImpl(
                Viewable.RAVEN_TABLE_MIMETYPE, dataConsumer.getTable());
        
        return Arrays.asList(table);
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
    
    public class RecordAsTableDataConsumer implements DataConsumer
    {
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

        public RecordAsTableDataConsumer(
                String[] fieldsOrder, String detailColumnName, String detailValueViewLinkName
                , String fieldNameColumnName
                , String fieldValueColumnName
                , Integer detailColumnNumber)
        {
            fields = new HashMap<String, RecordSchemaField>();
            this.recordSchema = RecordsAsTableNode.this.recordSchema;
            this.showFieldsInDetailColumn = RecordsAsTableNode.this.showFieldsInDetailColumn;
            this.detailColumnName = detailColumnName;
            this.detailValueViewLinkName = detailValueViewLinkName;
            this.fieldNameColumnName = fieldNameColumnName;
            this.fieldValueColumnName = fieldValueColumnName;
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

            int len = fieldNames.length +
                    (showFieldsInDetailColumn&&detailColumnNumber==null? 1 : 0);
            String[] columnNames = new String[len];
            for (int i=0; i<fieldNames.length; ++i)
            {
                String displayName = fields.get(fieldNames[i]).getDisplayName();
                columnNames[i] = displayName==null? fieldNames[i] : displayName;
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

        public void setData(DataSource dataSource, Object data)
        {
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
            
            int len = fieldNames.length + (showFieldsInDetailColumn&&detailColumnNumber<0? 1 : 0);
            Object[] row = new Object[len];
            for (int i=0; i<fieldNames.length; ++i)
                try
                {
                    String value = converter.convert(
                            String.class, record.getValue(fieldNames[i])
                            , fields.get(fieldNames[i]).getPattern());
                    if (showFieldsInDetailColumn && detailColumnNumber==i)
                        row[i] = createDetailObject(value, record);
                    else
                        row[i] = value;
                }
                catch (Exception e)
                {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error("Error adding record value, recieved from (%s), to table", e);
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
}
