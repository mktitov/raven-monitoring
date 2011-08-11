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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.raven.RavenUtils;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAsTableBaseAction extends AbstractActionNode implements DataSource
{
    public static final String PREPARE_ACTION_ATTRIBUTES_BINDING = "prepareActionAttributes";

    @Override
    public void prepareActionBindings(
            DataContext context, Map<String, Object> additionalBindings)
    {
    }

    @Override
    public ViewableObject createActionViewableObject(DataContext context, Map<String, Object> additionalBindings)
            throws Exception
    {
        return new RecordsAsTableRecordAction(this, context, additionalBindings, getActionAttributes(), this);
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        throw new UnsupportedOperationException("The pull operation not supported by this datasource");
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    protected Map<String, NodeAttribute> prepareFieldsAttributes(
            RecordSchema recordSchema, Record record, String fieldsOrder, boolean setAttrValue)
        throws Exception
    {
        Map<String, NodeAttribute> actionAttrs = getActionAttributes();
        Map<String, NodeAttribute> fieldsAttrs = new LinkedHashMap<String, NodeAttribute>();
        Map<String, RecordSchemaField> fields = RavenUtils.getRecordSchemaFields(recordSchema);
        if (fieldsOrder==null)
        {
            for (RecordSchemaField field: fields.values()){
                if (actionAttrs!=null && actionAttrs.containsKey(field.getName()))
                    fieldsAttrs.put(field.getName(), actionAttrs.get(field.getName()));
                else
                    fieldsAttrs.put(
                            field.getName(), createNodeAttribute(field, record, setAttrValue));
            }
        } else {
            String[] fieldNames = RavenUtils.split(fieldsOrder);
            for (String fieldName: fieldNames)
                if (fields.containsKey(fieldName)){
                    if (actionAttrs!=null && actionAttrs.containsKey(fieldName))
                        fieldsAttrs.put(fieldName, actionAttrs.get(fieldName));
                    else
                        fieldsAttrs.put(
                                fieldName, createNodeAttribute(fields.get(fieldName), record, setAttrValue));
                } else if (actionAttrs!=null && actionAttrs.containsKey(fieldName))
                    fieldsAttrs.put(fieldName, actionAttrs.get(fieldName));
        }
        addReferenceValuesToAttributes(fields.values(), fieldsAttrs);
        addValueValidatorsToAttributes(fields.values(), fieldsAttrs);

        bindingSupport.put(AbstractActionNode.ACTION_ATTRIBUTES_BINDING, fieldsAttrs);
        getNodeAttribute(PREPARE_ACTION_ATTRIBUTES_BINDING).getValue();

        return fieldsAttrs;
    }

    private NodeAttribute createNodeAttribute(
            RecordSchemaField field, Record record, boolean setAttrValue)
        throws Exception
    {
        String value = converter.convert(
                String.class, record.getValue(field.getName()), field.getPattern());
        NodeAttributeImpl attr = new NodeAttributeImpl(
                field.getName(), String.class, value, field.getPattern());
        attr.setDisplayName(field.getDisplayName());
        attr.setOwner(this);
        attr.init();

        return attr;
    }

    protected void addReferenceValuesToAttributes(
            Collection<RecordSchemaField> fields, Map<String, NodeAttribute> fieldsAttrs)
    {
        for (RecordSchemaField field: fields) {
            NodeAttribute attr = fieldsAttrs.get(field.getName());
            if (attr!=null)
                attr.setReferenceValuesSource(field.getReferenceValuesSource());
        }

        for (RecordFieldReferenceValuesNode valuesSource:
            NodeUtils.getChildsOfType(getEffectiveParent(), RecordFieldReferenceValuesNode.class))
        {
            NodeAttribute attr = fieldsAttrs.get(valuesSource.getFieldName());
            if (attr!=null)
                attr.setReferenceValuesSource(valuesSource.getReferenceValuesSource());
        }
    }

    protected void addValueValidatorsToAttributes(Collection<RecordSchemaField> fields
            , Map<String, NodeAttribute> fieldsAttrs)
    {
        for (RecordSchemaField field: fields) {
            NodeAttribute attr = fieldsAttrs.get(field.getName());
            if (attr!=null)
                attr.setValueValidatorController(field);
        }
    }
}
