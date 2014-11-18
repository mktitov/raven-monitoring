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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.DataStream;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class InitiatePushDataNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataConsumer pushDataTo;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object expressionForPushDataTo;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpressionForPushDataTo;

    public DataConsumer getPushDataTo() {
        return pushDataTo;
    }

    public void setPushDataTo(DataConsumer pushDataTo) {
        this.pushDataTo = pushDataTo;
    }

    public Object getExpressionForPushDataTo() {
        return expressionForPushDataTo;
    }

    public void setExpressionForPushDataTo(Object expressionForPushDataTo) {
        this.expressionForPushDataTo = expressionForPushDataTo;
    }

    public Boolean getUseExpressionForPushDataTo() {
        return useExpressionForPushDataTo;
    }

    public void setUseExpressionForPushDataTo(Boolean useExpressionForPushDataTo) {
        this.useExpressionForPushDataTo = useExpressionForPushDataTo;
    }

    @Override
    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        if (!dataSource.equals(pushDataTo))
            super.setData(dataSource, data, context);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception 
    {
        Object dataForPush = data;
        if (useExpressionForPushDataTo){
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(DATASOURCE_BINDING, dataSource);
            bindingSupport.put(SKIP_DATA_BINDING, SKIP_DATA);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            bindingSupport.put(DATA_STREAM_BINDING, new DataStreamForPushDataTo(pushDataTo, context, this));
            try{
                dataForPush = expressionForPushDataTo;
            } finally {
                bindingSupport.reset();
            }
        }
        if (!SKIP_DATA.equals(dataForPush))
            pushDataTo.setData(this, dataForPush, context);
        
        sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }

    private class DataStreamForPushDataTo implements DataStream {
        private final DataConsumer consumer;
        private final DataContext context;
        private final DataSource source;

        public DataStreamForPushDataTo(DataConsumer consumer, DataContext context, DataSource source) {
            this.consumer = consumer;
            this.context = context;
            this.source = source;
        }

        public DataStream push(Object data) {
            consumer.setData(source, data, context);
            return this;
        }

        public DataStream leftShift(Object data) {
            return push(data);
        }

        public DataContext getContext() {
            return context;
        }
    }
}
