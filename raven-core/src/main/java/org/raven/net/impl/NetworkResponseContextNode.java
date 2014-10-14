/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.net.impl;

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=NetworkResponseServiceNode.class, anyChildTypes=true)
public class NetworkResponseContextNode
       extends AbstractNetworkResponseContext implements DataConsumer
{
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    private ThreadLocal value;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void initFields() {
        super.initFields();
        value = new ThreadLocal();
    }

    public Object doGetResponse(String requesteIrp, Map<String, Object> params) throws NetworkResponseServiceExeption {
        return refereshData(null);
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        value.set(data);
        DataSourceHelper.executeContextCallbacks(this, context, data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        try {
            dataSource.getDataImmediate(this, new DataContextImpl(sessionAttributes));
            Object val = value.get();
            
            return val;
        } finally {
            value.remove();
        }
    }
}
