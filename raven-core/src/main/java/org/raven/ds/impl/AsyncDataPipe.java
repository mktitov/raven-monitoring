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
import org.raven.ds.DataContext;
import org.raven.ds.DataHandler;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class AsyncDataPipe extends AbstractAsyncDataPipe
{
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object handlerExpression;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useHandlerExpression;

    public Object getHandlerExpression() {
        return handlerExpression;
    }

    public void setHandlerExpression(Object handlerExpression) {
        this.handlerExpression = handlerExpression;
    }

    public Boolean getUseHandlerExpression() {
        return useHandlerExpression;
    }

    public void setUseHandlerExpression(Boolean useHandlerExpression) {
        this.useHandlerExpression = useHandlerExpression;
    }

    @Override
    public DataHandler createDataHandler() {
        return new Handler();
    }

    private class Handler implements DataHandler
    {
        public void releaseHandler() {
        }

        public Object handleData(Object data, DataSource dataSource, DataContext context, Node owner) 
                throws Exception
        {
            if (useHandlerExpression)
            {
                bindingSupport.put(DATA_BINDING, data);
                bindingSupport.put(DATASOURCE_BINDING, dataSource);
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                bindingSupport.put(SKIP_DATA_BINDING, SKIP_DATA);
                try{
                    return handlerExpression;
                }finally{
                    bindingSupport.reset();
                }
            }
            else
                return data;
        }

        public String getStatusMessage() {
            return "Sending data to consumers";
        }
    }
}
