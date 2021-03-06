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

import org.raven.annotations.Parameter;
import org.raven.ds.Record;
import org.raven.ds.RecordException;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RecordsAggregatorField extends BaseNode
{
    public final static String FIELD_VALUE_EXPRESSION_ATTR = "fieldValueExpression";

    @Parameter
    private String fieldName;
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object fieldValueExpression;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useFieldValueExpression;

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public Object getFieldValueExpression()
    {
        return fieldValueExpression;
    }

    public void setFieldValueExpression(Object fieldValueExpression)
    {
        this.fieldValueExpression = fieldValueExpression;
    }

    public Boolean getUseFieldValueExpression()
    {
        return useFieldValueExpression;
    }

    public void setUseFieldValueExpression(Boolean useFieldValueExpression)
    {
        this.useFieldValueExpression = useFieldValueExpression;
    }

    public Object getValue(Record record) throws RecordException
    {
        return useFieldValueExpression? fieldValueExpression : record.getValue(fieldName);
    }

}
