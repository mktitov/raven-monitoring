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

import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.DataStream;

/**
 *
 * @author Mikhail Titov
 */
public class DataStreamImpl implements DataStream
{
    private final DataSource source;
    private final DataContext context;
    private final DataConsumer consumer;

    public DataStreamImpl(DataSource source, DataContext context) {
        this(source, context, null);
    }
    
    public DataStreamImpl(DataSource source, DataContext context, DataConsumer consumer) {
        this.source = source;
        this.context = context;
        this.consumer = consumer;
    }

    @Override
    public DataStream push(Object data) {
        if (consumer!=null)
            consumer.setData(source, data, context);
        else 
            DataSourceHelper.sendDataToConsumers(source, data, context);
        return this;
    }

    @Override
    public DataStream leftShift(Object data) {
        return push(data);
    }

    @Override
    public DataContext getContext() {
        return context;
    }
}
