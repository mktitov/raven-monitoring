/*
 *  Copyright 2011 Mikhail Titov.
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
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.Record;
import org.raven.ds.RecordValidationErrors;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RecordValidatorNode extends AbstractSafeDataPipe
{
    public final static String ON_RECORD_VALIDATION_ERROR_ATTR = "onRecordValidationError";
    public final static String RECORD_BINDING = "record";
    public final static String ERRORS_BINDING = "errors";

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String onRecordValidationError;

    @Parameter(defaultValue="false")
    private Boolean useOnRecordValidationError;

    public String getOnRecordValidationError() {
        return onRecordValidationError;
    }

    public void setOnRecordValidationError(String onRecordValidationError) {
        this.onRecordValidationError = onRecordValidationError;
    }

    public Boolean getUseOnRecordValidationError() {
        return useOnRecordValidationError;
    }

    public void setUseOnRecordValidationError(Boolean useOnRecordValidationError) {
        this.useOnRecordValidationError = useOnRecordValidationError;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) 
        throws Exception
    {
        if (!(data instanceof Record)) {
            sendDataToConsumers(data, context);
            return;
        }

        Record record = (Record) data;
        RecordValidationErrors errors = record.validate();
        if (errors!=null){
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn(errors.toText());
            data=null;
            if (useOnRecordValidationError){
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                bindingSupport.put(DATASOURCE_BINDING, dataSource);
                bindingSupport.put(RECORD_BINDING, record);
                bindingSupport.put(ERRORS_BINDING, errors);
                try {
                    data = getAttr(ON_RECORD_VALIDATION_ERROR_ATTR).getRealValue();
                    if (data==null) {
                        context.addError(this, errors.toText());
                    }
                } finally {
                    bindingSupport.reset();
                }
            }
        } 
        if (data!=null)
            sendDataToConsumers(data, context);
        else
            DataSourceHelper.executeContextCallbacks(this, context, record);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data
            , DataContext context, BindingSupport bindingSupport)
    {
    }
}