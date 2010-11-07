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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Collection;
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.ds.SessionAttributeGenerator;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSafeDataPipe extends AbstractDataSource implements DataPipe
{
    public static final String DATA_CONTEXT_BINDING = "context";
    public static final String DATASOURCE_BINDING = "dataSource";
    public static final String DATA_BINDING = "data";
    public static final String EXPRESSION_ATTRIBUTE = "expression";
    public static final String PREPROCESS_ATTRIBUTE = "preProcess";
    public static final String SESSIONATTRIBUTES_BINDING = "sessAttrs";

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean forwardDataSourceAttributes;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String preProcess;

    @NotNull @Parameter(defaultValue="false")
    private Boolean usePreProcess;

    protected ThreadLocal<DataConsumer> consumer;
    protected BindingSupportImpl bindingSupport;

    public String getPreProcess()
    {
        return preProcess;
    }

    public void setPreProcess(String preProcess)
    {
        this.preProcess = preProcess;
    }

    public Boolean getUsePreProcess()
    {
        return usePreProcess;
    }

    public void setUsePreProcess(Boolean usePreProcess)
    {
        this.usePreProcess = usePreProcess;
    }

    public Boolean getForwardDataSourceAttributes()
    {
        return forwardDataSourceAttributes;
    }

    public void setForwardDataSourceAttributes(Boolean forwardDataSourceAttributes)
    {
        this.forwardDataSourceAttributes = forwardDataSourceAttributes;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public Object getExpression()
    {
        return expression;
    }

    public void setExpression(Object expression)
    {
        this.expression = expression;
    }

    public Boolean getUseExpression()
    {
        return useExpression;
    }

    public void setUseExpression(Boolean useExpression)
    {
        this.useExpression = useExpression;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        consumer = new ThreadLocal<DataConsumer>();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected boolean allowAttributesGeneration(NodeAttribute attr)
    {
        if (   attr.getName().equals(DATASOURCE_ATTRIBUTE)
            && forwardDataSourceAttributes!=null
            && forwardDataSourceAttributes)
        {
            return false;
        }
        else
            return super.allowAttributesGeneration(attr);
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, DataContext context) throws Exception
    {
        consumer.set(dataConsumer);
        try
        {
            Collection<Node> childs = getEffectiveChildrens();
            if (childs!=null && !childs.isEmpty())
                for (Node child: childs)
                    if (   child.getStatus().equals(Status.STARTED)
                        && child instanceof SessionAttributeGenerator)
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug(String.format(
                                    "Creating session attribute (%s)", child.getName()));
                        SessionAttributeGenerator gen = (SessionAttributeGenerator)child;
                        Object value = gen.getFieldValue(context);
                        NodeAttributeImpl attr = new NodeAttributeImpl(
                                gen.getName(), gen.getAttributeType(), value, null);
                        attr.setOwner(this);
                        attr.init();
                        context.addSessionAttribute(attr);
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug(String.format(
                                    "Attribute information: type - (%s), value (%s)"
                                    , gen.getAttributeType(), value));
                    }

            Object preprocessResult = null;
            if (usePreProcess)
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Preprocessing...");
                bindingSupport.put(SESSIONATTRIBUTES_BINDING, context.getSessionAttributes());
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                try
                {
                    preprocessResult = getNodeAttribute(PREPROCESS_ATTRIBUTE).getRealValue();
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format("Preprocessed value is (%s)", preprocessResult));
                }
                finally
                {
                    bindingSupport.reset();
                }
            }
            boolean result = true;
            if (preprocessResult==null)
                result = dataSource.getDataImmediate(this, context);
            else
                setData(this, preprocessResult, context);
            
            return result;
        }
        finally
        {
            consumer.remove();
        }
    }

    @Override
    public Collection<NodeAttribute> generateAttributes()
    {
        Collection<NodeAttribute> consumerAttributes = new ArrayList<NodeAttribute>();
        Boolean _forwardDataSourceAttributes = forwardDataSourceAttributes;
        DataSource _dataSource = getDataSource();
        if (_forwardDataSourceAttributes!=null && _forwardDataSourceAttributes && _dataSource!=null)
        {
            Collection<NodeAttribute> dsAttrs = _dataSource.generateAttributes();
            if (dsAttrs!=null && !dsAttrs.isEmpty())
                consumerAttributes.addAll(dsAttrs);
        }

        Collection<Node> childs = getEffectiveChildrens();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                if (   child.getStatus().equals(Status.STARTED)
                    && child instanceof SessionAttributeGenerator)
                {
                    ((SessionAttributeGenerator)child).fillConsumerAttributes(consumerAttributes);
                }

        return consumerAttributes.isEmpty()? null : consumerAttributes;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        if (!Status.STARTED.equals(getStatus()))
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format(
                        "Can't recieve DATA from data source (%s). Node not STARTED", dataSource.getPath()));
            return;
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Recieved data from the ({}). Processing...", dataSource.getPath());
        if (useExpression)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Using expression to transform data", dataSource.getPath());
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(SKIP_DATA_BINDING, SKIP_DATA);
            bindingSupport.put(DATASOURCE_BINDING, dataSource);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            doAddBindingsForExpression(dataSource, data, context, bindingSupport);
            try
            {
                NodeAttribute exprAttr = getNodeAttribute(EXPRESSION_ATTRIBUTE);
                data = exprAttr.getRealValue();
            }
            finally
            {
                bindingSupport.reset();
            }
        }
        try
        {
            if (data!=SKIP_DATA)
                doSetData(dataSource, data, context);
            else if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Expression return SKIP_DATA. Terminating push data process");
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format("Error handling data from (%s) data source", dataSource.getPath())
                    , e);
        }
    }

    protected abstract void doSetData(DataSource dataSource, Object data, DataContext context)
            throws Exception;

    protected abstract void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport);

    @Override
    public void sendDataToConsumers(Object data, DataContext context)
    {
        if (consumer.get()!=null) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Pushing data ONLY to consumer ({})", consumer.get().getPath());
            consumer.get().setData(this, data, context);
        }
        else
        {
            Collection<Node> deps = getDependentNodes();
            if (deps!=null && !deps.isEmpty())
                for (Node dep: deps)
                    if (dep instanceof DataConsumer && Status.STARTED.equals(dep.getStatus())){
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            getLogger().debug("Pushing data to the consumer ({})", dep.getPath());
                        ((DataConsumer)dep).setData(this, data, context);
                    }
        }
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

}
