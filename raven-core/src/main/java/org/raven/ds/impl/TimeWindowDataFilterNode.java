/*
 * Copyright 2012 Mikhail Titov.
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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.sched.impl.TimeWindowNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes=TimeWindowNode.class)
public class TimeWindowDataFilterNode extends AbstractSafeDataPipe {

    @NotNull @Parameter(defaultValue="false")
    private Boolean invertResult;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean filterOnPull;
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean filterOnPush;
    
    @Override
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        return isCurrentTimeInPeriod() || !filterOnPull? super.getDataImmediate(dataConsumer, context) : false;
    }

    @Override
    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (isCurrentTimeInPeriod() || !filterOnPush)
            super.setData(dataSource, data, context);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context
               , BindingSupport bindingSupport) 
    {
    }
    
    public boolean isCurrentTimeInPeriod() {
        if (!Status.STARTED.equals(getStatus()))
            return false;
        boolean res = false;
        for (TimeWindowNode node: NodeUtils.getChildsOfType(this, TimeWindowNode.class))
            if (node.isCurrentTimeInPeriod()) {
                res = true;
                break;
            }
        return invertResult? !res : res;
    }

    public Boolean getFilterOnPull() {
        return filterOnPull;
    }

    public void setFilterOnPull(Boolean filterOnPull) {
        this.filterOnPull = filterOnPull;
    }

    public Boolean getFilterOnPush() {
        return filterOnPush;
    }

    public void setFilterOnPush(Boolean filterOnPush) {
        this.filterOnPush = filterOnPush;
    }

    public Boolean getInvertResult() {
        return invertResult;
    }

    public void setInvertResult(Boolean invertResult) {
        this.invertResult = invertResult;
    }
}
