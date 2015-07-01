/*
 * Copyright 2014 Mikhail Titov.
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

import java.util.concurrent.TimeUnit;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class TimeLoopDataPipeNode extends AbstractSafeDataPipe {
    @NotNull @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @NotNull @Parameter
    private Long delayTime;
    
    @NotNull @Parameter(defaultValue = "SECONDS")
    private TimeUnit delayTimeUnit;

    @Override
    protected void doSetData(DataSource dataSource, final Object data, final DataContext context) throws Exception {
        executor.execute(delayTimeUnit.toMillis(delayTime), new AbstractTask(this, "Wating for push data") {
            @Override
            public void doRun() throws Exception {
                sendDataToConsumers(data, context);
            }
        });
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, 
            BindingSupport bindingSupport) 
    {
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Long getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(Long delayTime) {
        this.delayTime = delayTime;
    }

    public TimeUnit getDelayTimeUnit() {
        return delayTimeUnit;
    }

    public void setDelayTimeUnit(TimeUnit delayTimeUnit) {
        this.delayTimeUnit = delayTimeUnit;
    }
}
