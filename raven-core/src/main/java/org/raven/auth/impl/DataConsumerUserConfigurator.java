/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.auth.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContextConfig;
import org.raven.auth.UserContextConfigurator;
import org.raven.auth.UserContextConfiguratorException;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=UserContextConfiguratorsNode.class)
public class DataConsumerUserConfigurator extends BaseNode implements DataConsumer, UserContextConfigurator, BindingNames {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object beforeConfigure;
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object configure;
    
    private ThreadLocal<List> dataHolder;
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
        dataHolder = new ThreadLocal<List>() {
            @Override protected List initialValue() {
                return new LinkedList();
            }
        };
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {
        if (data!=null)
            dataHolder.get().add(data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Not supported operation for this data consumer.");
    }

    public void configure(UserContextConfig userContext) throws UserContextConfiguratorException {
        if (!isStarted())
            return;
        dataHolder.remove();
        try {
            DataContext context = new DataContextImpl();
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            bindingSupport.put(USER_CONTEXT_CONFIGURATOR_BINDING, userContext);
            getBeforeConfigure();
            dataSource.getDataImmediate(this, context);
            bindingSupport.put(DATA_BINDING, dataHolder.get());
            getConfigure();
        } finally {
            dataHolder.remove();
            bindingSupport.reset();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Object getBeforeConfigure() {
        return beforeConfigure;
    }

    public void setBeforeConfigure(Object beforeConfigure) {
        this.beforeConfigure = beforeConfigure;
    }

    public Object getConfigure() {
        return configure;
    }

    public void setConfigure(Object configure) {
        this.configure = configure;
    }

}
