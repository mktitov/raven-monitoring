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
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class ChangeRouteNode extends SafeDataPipeNode
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataConsumer routeDataTo;

    public DataConsumer getRouteDataTo() {
        return routeDataTo;
    }

    public void setRouteDataTo(DataConsumer routeDataTo) {
        this.routeDataTo = routeDataTo;
    }
    
    @Override
    public void sendDataToConsumers(Object data, DataContext context)
    {
        DataConsumer consumer = (DataConsumer) context.getNodeParameter(this, CONSUMER_PARAM);
        if (consumer!=null)
            routeDataTo.setData(this, data, context);
        else
            super.sendDataToConsumers(data, context);
    }
}
