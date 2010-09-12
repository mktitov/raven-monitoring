/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordsAsTableNode.class)
public class EditRecordActionNode extends RecordsAsTableRecordActionNode
{
    public static final String RECORD_BINDING = "record";
    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchema recordSchema;

    @Parameter
    private String fieldsOrder;

    @Message
    private static String editRecordErrorMessage;

    @Override
    public ViewableObject createActionViewableObject(DataContext context, Map<String, Object> additionalBindings)
            throws Exception
    {
        Record record = (Record) additionalBindings.get(RecordsAsTableNode.RECORD_BINDING);
        Map<String, NodeAttribute> actionAttrs = getActionAttributes();
        Map<String, NodeAttribute> fieldsAttrs = new LinkedHashMap<String, NodeAttribute>();

        bindingSupport.put(RECORD_BINDING, record);
        
        String _fieldsOrder = fieldsOrder;
        if (_fieldsOrder==null)
        {
            RecordSchemaField[] fields = recordSchema.getFields();
            if (fields!=null)
                for (RecordSchemaField field: fields){
                    if (actionAttrs!=null && actionAttrs.containsKey(field.getName()))
                        fieldsAttrs.put(field.getName(), actionAttrs.get(field.getName()));
                    else {
                        fieldsAttrs.put(field.getName(), createNodeAttribute(field, record));
                    }
                }
        } else {
            String[] fieldNames = RavenUtils.split(_fieldsOrder);
            Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(recordSchema);
            for (String fieldName: fieldNames)
                if (fields.containsKey(fieldName)){
                    if (actionAttrs!=null && actionAttrs.containsKey(fieldName))
                        fieldsAttrs.put(fieldName, actionAttrs.get(fieldName));
                    else
                        fieldsAttrs.put(fieldName, createNodeAttribute(fields.get(fieldName), record));
                } else if (actionAttrs!=null && actionAttrs.containsKey(fieldName))
                    fieldsAttrs.put(fieldName, actionAttrs.get(fieldName));
        }
        return new AddEditRecordAction(this, context, additionalBindings, fieldsAttrs, editRecordErrorMessage, this, recordSchema);
    }

    private NodeAttribute createNodeAttribute(RecordSchemaField field, Record record) throws Exception
    {
        String value = converter.convert(String.class, record.getValue(field.getName()), field.getPattern());
        NodeAttributeImpl attr = new NodeAttributeImpl(field.getName(), String.class, value, field.getPattern());
        attr.setDisplayName(field.getDisplayName());
        attr.setOwner(this);
        attr.init();

        return attr;
    }

    public String getFieldsOrder() {
        return fieldsOrder;
    }

    public void setFieldsOrder(String fieldsOrder) {
        this.fieldsOrder = fieldsOrder;
    }

    public RecordSchema getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(RecordSchema recordSchema) {
        this.recordSchema = recordSchema;
    }

}
