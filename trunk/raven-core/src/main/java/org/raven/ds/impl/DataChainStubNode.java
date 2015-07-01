/*
 * Copyright 2013 Mikhail TItov.
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

import java.util.Collection;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = DataChainNode.class)
public class DataChainStubNode extends BaseNode implements DataConsumer, BindingNames {
    @NotNull @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    @NotNull @Parameter(defaultValue = "true")
    private Boolean autoLinkDataSource;

    @Override
    public boolean start() throws NodeError {
        if (autoLinkDataSource!=null && autoLinkDataSource && getAttr(DATASOURCE_BINDING).getRawValue()==null)
            NodeUtils.reconnectDataSources(getParent());
        return super.start();
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        DataChainNode chain = (DataChainNode) getEffectiveParent();
        chain.dataProcessedByChain(data, context);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Not supported operation");
    }

    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue, Object newValue) {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        
        if (   node==this 
            && ObjectUtils.in(getStatus(), Status.INITIALIZED, Status.STARTED)
            && attr.getName().equals(AbstractDataConsumer.AUTO_LINK_DATA_SOURCE_ATTR)
            && newValue!=null && (Boolean)newValue)
        {
            NodeUtils.reconnectDataSources(getParent());
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Boolean getAutoLinkDataSource() {
        return autoLinkDataSource;
    }

    public void setAutoLinkDataSource(Boolean autoLinkDataSource) {
        this.autoLinkDataSource = autoLinkDataSource;
    }
}
