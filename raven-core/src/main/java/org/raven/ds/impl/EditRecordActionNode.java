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
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.AbstractActionNode;
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
    public static final String PREPARE_RECORD_BINDING = "prepareRecord";

    @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    @NotNull
    private RecordSchema recordSchema;

    @Parameter
    private String fieldsOrder;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String prepareRecord;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String prepareActionAttributes;
    
    @Message
    private static String editRecordErrorMessage;

    @Override
    public ViewableObject createActionViewableObject(
            DataContext context, Map<String, Object> additionalBindings)
        throws Exception
    {
        Record record = (Record) additionalBindings.get(RecordsAsTableNode.RECORD_BINDING);
        bindingSupport.put(RecordsAsTableNode.RECORD_BINDING, record);

        getNodeAttribute(PREPARE_RECORD_BINDING).getValue();
        
        Map<String, NodeAttribute> fieldsAttrs =
                prepareFieldsAttributes(recordSchema, record, fieldsOrder, true);

        return new AddEditRecordAction(this, context, additionalBindings, fieldsAttrs, editRecordErrorMessage, this, recordSchema);
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

    public String getPrepareActionAttributes() {
        return prepareActionAttributes;
    }

    public void setPrepareActionAttributes(String prepareActionAttributes) {
        this.prepareActionAttributes = prepareActionAttributes;
    }

    public String getPrepareRecord() {
        return prepareRecord;
    }

    public void setPrepareRecord(String prepareRecord) {
        this.prepareRecord = prepareRecord;
    }
}
