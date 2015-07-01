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
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class SynchronizedDataPipe extends AbstractSafeDataPipe {

    @Override
    public synchronized void setData(DataSource dataSource, Object data, DataContext context) {
        super.setData(dataSource, data, context);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }
}
