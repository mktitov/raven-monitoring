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
public class InitiatePullDataNode extends AbstractSafeDataPipe
{
    public enum DataMixPolicy {PASS_BOTH, PATH_DATASOURCE, PASS_NEW_DATASOURCE}
    public enum DataOrderPolicy {FIRST_DATASOURCE, FIRST_NEW_DATASOURCE}

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource pullDataFrom;

    @NotNull @Parameter(defaultValue="PASS_BOTH")
    private DataMixPolicy dataMixPolicy;

    @NotNull @Parameter(defaultValue="FIRST_NEW_DATASOURCE")
    private DataOrderPolicy dataOrderPolicy;

    public DataOrderPolicy getDataOrderPolicy() {
        return dataOrderPolicy;
    }

    public void setDataOrderPolicy(DataOrderPolicy dataOrderPolicy) {
        this.dataOrderPolicy = dataOrderPolicy;
    }

    public DataMixPolicy getDataMixPolicy() {
        return dataMixPolicy;
    }

    public void setDataMixPolicy(DataMixPolicy dataMixPolicy) {
        this.dataMixPolicy = dataMixPolicy;
    }

    public DataSource getPullDataFrom() {
        return pullDataFrom;
    }

    public void setPullDataFrom(DataSource pullDataFrom) {
        this.pullDataFrom = pullDataFrom;
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (dataOrderPolicy==DataOrderPolicy.FIRST_NEW_DATASOURCE && !dataSource.equals(pullDataFrom))
            pullDataFrom.getDataImmediate(this, context);
        DataMixPolicy policy = dataMixPolicy;
        if (   policy==DataMixPolicy.PASS_BOTH
            || (dataSource.equals(getDataSource()) && policy==DataMixPolicy.PATH_DATASOURCE)
            || (dataSource.equals(pullDataFrom) && policy==DataMixPolicy.PASS_NEW_DATASOURCE))
        {
            sendDataToConsumers(data, context);
        }
        if (dataOrderPolicy==DataOrderPolicy.FIRST_DATASOURCE && !dataSource.equals(pullDataFrom))
            pullDataFrom.getDataImmediate(this, context);
    }
}
