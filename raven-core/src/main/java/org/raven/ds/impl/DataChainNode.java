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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.tree.Node;
import static org.raven.util.NodeUtils.*;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(anyChildTypes = true)
public class DataChainNode extends AbstractSafeDataPipe {
    public static final String FIRST_CHAIN_CONSUMER_PARAM = "firstChainConsumer";
    
    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
//        DataConsumer firstCons = (DataConsumer) context.getNodeParameter(this, FIRST_CHAIN_CONSUMER_PARAM);
//        if (firstCons==null) {
//            firstCons = getFirstConsumerInChain();
//            if (firstCons==null) throw new Exception("Not found first dataConsumer in chain");
//            else context.putNodeParameter(this, FIRST_CHAIN_CONSUMER_PARAM, firstCons);
//        }
//        firstCons.setData(this, data, context);
        getFirstConsumerInChain(context).setData(this, data, context);
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
}
