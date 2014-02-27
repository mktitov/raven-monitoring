/*
 * Copyright 2013 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.ds.impl;

import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.tree.Node;
import static org.raven.util.NodeUtils.*;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes = true)
public class DataChainNode extends AbstractSafeDataPipe {
    
    public static final String FIRST_CHAIN_CONSUMER_PARAM = "firstChainConsumer";
    public static final String USER_CHAIN_ATTR = "useChain";
    
    @NotNull @Parameter(defaultValue = "true")
    private Boolean useChain;
    
    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        final DataConsumer firstChainConsumer = getFirstConsumerInChain(context);
        if (isUsingChain(dataSource, data, context))
            firstChainConsumer.setData(this, data, context);
        else
            DataSourceHelper.sendDataToConsumers(this, data, context, firstChainConsumer);
    }
    
    private boolean isUsingChain(DataSource dataSource, Object data, DataContext context) {
        if (getAttr("useChain").getValueHandler() instanceof ExpressionAttributeValueHandler) {
            bindingSupport.put(BindingNames.DATASOURCE_BINDING, dataSource);
            bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
            bindingSupport.put(BindingNames.DATA_BINDING, data);
            try {
                return useChain;
            } finally {
                bindingSupport.reset();
            }
        } else
            return useChain;
    }
    
    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }
    
    private DataConsumer getFirstConsumerInChain(DataContext context) throws Exception {
        DataConsumer firstCons = (DataConsumer) context.getNodeParameter(this, FIRST_CHAIN_CONSUMER_PARAM);
        if (firstCons!=null) return firstCons;
        for (DataConsumer cons: extractNodesOfType(getDependentNodes(), DataConsumer.class))
            if (cons instanceof Node && ((Node)cons).getEffectiveParent().equals(this)) {
                context.putNodeParameter(this, FIRST_CHAIN_CONSUMER_PARAM, cons);
                return cons;
            }
        throw new Exception("Not found first dataConsumer in chain");
    }
    
    public void dataProcessedByChain(Object data, DataContext context) {
        try {
            sendError(data, context);
            DataSourceHelper.sendDataToConsumers(this, data, context, getFirstConsumerInChain(context));
        } catch (Exception ex) {
            context.addError(this, ex);
            sendError(data, context);
        }
    }

    public Boolean getUseChain() {
        return useChain;
    }

    public void setUseChain(Boolean useChain) {
        this.useChain = useChain;
    }
}
