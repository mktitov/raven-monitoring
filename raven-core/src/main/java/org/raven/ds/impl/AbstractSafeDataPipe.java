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
import org.raven.BindingNames;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.ds.SessionAttributeGenerator;
import org.raven.expr.BindingSupport;
import org.raven.expr.VarsSupportState;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractSafeDataPipe 
        extends AbstractDataSource implements DataPipe, BindingNames
{
    public static final String EXPRESSION_ATTRIBUTE = "expression";
    public static final String PREPROCESS_ATTRIBUTE = "preProcess";

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataConsumer errorConsumer;
    
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

    @NotNull @Parameter(defaultValue="false")
    private Boolean autoLinkDataSource;

    protected BindingSupportImpl bindingSupport;
    private boolean supportedPullOperation = true;
    private boolean hasSessAttrGenChildType = false;

    protected boolean isSupportedPullOperation() {
        return supportedPullOperation;
    }

    protected void setSupportedPullOperation(boolean supportedPullOperation) {
        this.supportedPullOperation = supportedPullOperation;
    }

    public Boolean getAutoLinkDataSource() {
        return autoLinkDataSource;
    }

    public void setAutoLinkDataSource(Boolean autoLinkDataSource) {
        this.autoLinkDataSource = autoLinkDataSource;
    }

    public String getPreProcess() {
        return preProcess;
    }

    public void setPreProcess(String preProcess) {
        this.preProcess = preProcess;
    }

    public Boolean getUsePreProcess() {
        return usePreProcess;
    }

    public void setUsePreProcess(Boolean usePreProcess) {
        this.usePreProcess = usePreProcess;
    }

    public Boolean getForwardDataSourceAttributes() {
        return forwardDataSourceAttributes;
    }

    public void setForwardDataSourceAttributes(Boolean forwardDataSourceAttributes) {
        this.forwardDataSourceAttributes = forwardDataSourceAttributes;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataConsumer getErrorConsumer() {
        return errorConsumer;
    }

    public void setErrorConsumer(DataConsumer errorConsumer) {
        this.errorConsumer = errorConsumer;
    }

    public Object getExpression() {
        return expression;
    }

    public void setExpression(Object expression) {
        this.expression = expression;
    }

    public Boolean getUseExpression() {
        return useExpression;
    }

    public void setUseExpression(Boolean useExpression) {
        this.useExpression = useExpression;
    }

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        for (Class nodeType: getChildNodeTypes()) 
            if (SessionAttributeGenerator.class.isAssignableFrom(nodeType)) {
                hasSessAttrGenChildType = true;
                break;
            }
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
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        if (supportedPullOperation)
            return super.getDataImmediate(dataConsumer, context);
        else {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("The pipe does not supports pull operation, "
                        + "so ignoring request from the ({})", dataConsumer.getPath());
            return false;
        }
    }

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context) throws Exception {
        if (dataConsumer!=null)
            context.putNodeParameter(this, CONSUMER_PARAM, dataConsumer);

        if (hasSessAttrGenChildType)
            for (SessionAttributeGenerator gen: NodeUtils.getEffectiveChildsOfType(this, SessionAttributeGenerator.class)) {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format("Creating session attribute (%s)", gen.getName()));
                Object value = gen.getFieldValue(context);
                NodeAttributeImpl attr = new NodeAttributeImpl(
                        gen.getName(), gen.getAttributeType(), value, null);
                attr.setOwner(this);
                attr.init();
                context.addSessionAttribute(attr);
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                        "Attribute information: type - (%s), value (%s)", gen.getAttributeType(), value));
            }
        Object preprocessResult = null;
        if (usePreProcess) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug("Preprocessing...");
            VarsSupportState varsSupportState = NodeUtils.resetVarsSupport(tree);
            try {
                bindingSupport.put(SESSIONATTRIBUTES_BINDING, context.getSessionAttributes());
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                bindingSupport.put(REQUESTER_BINDING, dataConsumer);
                bindingSupport.put(SKIP_DATA_BINDING, SKIP_DATA);
                try {
                    preprocessResult = getAttr(PREPROCESS_ATTRIBUTE).getRealValue();
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format("Preprocessed value is (%s)", preprocessResult));
                } finally {
                    bindingSupport.reset();
                }
            } finally {
                varsSupportState.restoreState();
            }
        }
        boolean result = true;
        if (preprocessResult==null) 
            try {
                bindingSupport.put(SESSIONATTRIBUTES_BINDING, context.getSessionAttributes());
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                bindingSupport.put(REQUESTER_BINDING, dataConsumer);
                result = dataSource.getDataImmediate(this, context);
            } finally {
                bindingSupport.reset();
            }
        else if (preprocessResult==SKIP_DATA) {
            if (logger.isDebugEnabled())
                logger.debug("preProcess returns SKIP_DATA. Skipping...");
        } else
            setData(this, preprocessResult, context);

        return result;
    }
    
    @Override
    public Collection<NodeAttribute> generateAttributes() {
        Collection<NodeAttribute> consumerAttributes = new ArrayList<NodeAttribute>();
        Boolean _forwardDataSourceAttributes = forwardDataSourceAttributes;
        DataSource _dataSource = getDataSource();
        if (_forwardDataSourceAttributes!=null && _forwardDataSourceAttributes && _dataSource!=null)
        {
            Collection<NodeAttribute> dsAttrs = _dataSource.generateAttributes();
            if (dsAttrs!=null && !dsAttrs.isEmpty())
                consumerAttributes.addAll(dsAttrs);
        }

        Collection<Node> childs = getEffectiveNodes();
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
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        final boolean debugEnabled = isLogLevelEnabled(LogLevel.DEBUG);
        if (!isStarted()) {
            if (debugEnabled)
                debug(String.format(
                        "Can't recieve DATA from data source (%s). Node not STARTED", dataSource.getPath()));
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        }
        final boolean _stopProcessingOnError = getStopProcessingOnError();
        if (context.hasErrors() && _stopProcessingOnError) {
            if (debugEnabled)
                debug(String.format(
                        "Data context has error flag and stopProcessingOnError is true, so IGNORING", 
                        dataSource.getPath()));
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        }
        VarsSupportState varsSupportState = NodeUtils.resetVarsSupport(tree);
        try {
            if (debugEnabled)
                getLogger().debug("Recieved data from the ({}). Processing...", dataSource.getPath());
            if (useExpression) {
                if (debugEnabled)
                    getLogger().debug("Using expression to transform data", dataSource.getPath());
                bindingSupport.put(DATA_BINDING, data);
                bindingSupport.put(SKIP_DATA_BINDING, SKIP_DATA);
                bindingSupport.put(DATASOURCE_BINDING, dataSource);
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                bindingSupport.put(DATA_STREAM_BINDING, new DataStreamImpl(this, context));
                doAddBindingsForExpression(dataSource, data, context, bindingSupport);
                try {
                    NodeAttribute exprAttr = getAttr(EXPRESSION_ATTRIBUTE);
                    data = exprAttr.getRealValue();
                } finally {
                    bindingSupport.reset();
                }
            }
            try  {
                if (context.hasErrors() && _stopProcessingOnError) 
                    sendError(data, context);
                else if (data!=SKIP_DATA)
                    doSetData(dataSource, data, context);
                else if (debugEnabled)
                    getLogger().debug("Expression return SKIP_DATA. Terminating push data process");
            } catch (Throwable e) {
                context.addError(this, e);
                sendError(data, context);
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(
                        String.format("Error handling data from (%s) data source", dataSource.getPath())
                        , e);
            }
        } finally {
            varsSupportState.restoreState();
        }
    }

    protected abstract void doSetData(DataSource dataSource, Object data, DataContext context)
            throws Exception;

    protected abstract void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport);

    @Override
    public void sendDataToConsumers(Object data, DataContext context) {
        super.sendDataToConsumers(data, context);
//        DataSourceHelper.sendDataToConsumers(this, data, context);
    }
    
    /**
     * 
     * @return Returns <b>false</b> if pipe must stop sending data to consumers.
     */
    protected boolean sendDataAndError(Object data, DataContext context, boolean stopProcessingOnError, 
            DataConsumer errorConsumer) 
    {
        if (context.hasErrors() && stopProcessingOnError) return false;        
        sendDataToConsumers(data, context);
        if (!context.hasErrors() || !stopProcessingOnError) return true;
        else {
            sendError(data, context, errorConsumer, stopProcessingOnError);
            return false;
        }
    }

//    @Override
//    public void nodeIndexChanged(Node node, int oldIndex, int newIndex)
//    {
//        if (   ObjectUtils.in(getStatus(), Status.INITIALIZED, Status.STARTED)
//            && autoLinkDataSource!=null && autoLinkDataSource)
//        {
//            NodeUtils.reconnectDataSources(getParent());
//        }
//    }

    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        
        if (   ObjectUtils.in(getStatus(), Status.INITIALIZED, Status.STARTED)
            && attr.getName().equals(AbstractDataConsumer.AUTO_LINK_DATA_SOURCE_ATTR)
            && newValue!=null && (Boolean)newValue)
        {
            NodeUtils.reconnectDataSources(getParent());
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

    protected boolean sendError(Object data, DataContext context) {
        return sendError(data, context, errorConsumer, getStopProcessingOnError());
    }
    
    protected boolean sendError(Object data, DataContext context, DataConsumer errorConsumer, 
            boolean stopProcessingOnError)
    {
        if (context.hasErrors()) 
            DataSourceHelper.executeContextCallbacks(this, context, data);
        if (stopProcessingOnError && errorConsumer!=null && context.hasErrors()) {
            errorConsumer.setData(this, data, context);
            return true;
        } else
            return false;
            
    }
}
