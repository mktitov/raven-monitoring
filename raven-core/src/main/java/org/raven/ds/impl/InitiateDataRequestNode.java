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
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class InitiateDataRequestNode extends AbstractSafeDataPipe
{
    public enum DataMixPolicy {PASS_BOTH, PATH_DATASOURCE, PASS_NEW_DATASOURCE}

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource initiateDataRequestFrom;

    @NotNull @Parameter(defaultValue="PASS_BOTH")
    private DataMixPolicy dataMixPolicy;

    public DataMixPolicy getDataMixPolicy() {
        return dataMixPolicy;
    }

    public void setDataMixPolicy(DataMixPolicy dataMixPolicy) {
        this.dataMixPolicy = dataMixPolicy;
    }

    public DataSource getInitiateDataRequestFrom() {
        return initiateDataRequestFrom;
    }

    public void setInitiateDataRequestFrom(DataSource initiateDataRequestFrom) {
        this.initiateDataRequestFrom = initiateDataRequestFrom;
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (!dataSource.equals(initiateDataRequestFrom))
            initiateDataRequestFrom.getDataImmediate(this, context);
        DataMixPolicy policy = dataMixPolicy;
        if (   policy==DataMixPolicy.PASS_BOTH
            || (dataSource.equals(getDataSource()) && policy==DataMixPolicy.PATH_DATASOURCE)
            || (dataSource.equals(initiateDataRequestFrom) && policy==DataMixPolicy.PASS_NEW_DATASOURCE))
        {
            sendDataToConsumers(data, context);
        }
    }
}
