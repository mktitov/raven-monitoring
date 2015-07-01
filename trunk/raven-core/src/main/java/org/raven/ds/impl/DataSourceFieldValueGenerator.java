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

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordGeneratorNode.class, anyChildTypes=true)
public class DataSourceFieldValueGenerator
        extends AbstractFieldValueGenerator implements DataConsumer
{
    public final static String DATASOURCE_ATTR = "dataSource";
    public static final String DATA_BINDING = "data";
    public static final String DATA_CONTEXT_BINDING = "context";
    public static final String EXPRESSION_ATTR = "expression";
    public static final String PREPROCESS_ATTR = "preProcess";

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private DataSource dataSource;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String preProcess;

    @NotNull @Parameter(defaultValue="false")
    private Boolean usePreProcess;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean executeContextCallbacks;

    private ThreadLocal<DataInfo> dataInfo;

    public Boolean getExecuteContextCallbacks() {
        return executeContextCallbacks;
    }

    public void setExecuteContextCallbacks(Boolean executeContextCallbacks) {
        this.executeContextCallbacks = executeContextCallbacks;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Boolean getUseExpression() {
        return useExpression;
    }

    public void setUseExpression(Boolean useExpression) {
        this.useExpression = useExpression;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
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

    @Override
    protected void initFields() {
        super.initFields();
        dataInfo = new ThreadLocal();
    }
    
    public void setData(DataSource dataSource, Object data, DataContext context)
    {
        if (data!=null || dataInfo.get()==null)
            dataInfo.set(new DataInfo(data, context));
        if (executeContextCallbacks)
            DataSourceHelper.executeContextCallbacks(this, context, data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object doGetFieldValue(DataContext context) {
        dataInfo.remove();
        Object val = preProcess(context);
        if (val!=null)
            return val;
        dataSource.getDataImmediate(this, context);
        DataInfo info = dataInfo.get();
        val = info==null? null : info.data;
        DataContext _context = info==null || info.context==null? context : info.context;
        dataInfo.remove();
        if (useExpression) {
            bindingSupport.put(DATA_BINDING, val);
            bindingSupport.put(DATA_CONTEXT_BINDING, _context);
            val = getAttr(EXPRESSION_ATTR).getRealValue();
        }
        return val;
    }
    
    private Object preProcess(DataContext context) {
        Boolean _usePreProcess = usePreProcess;
        if (_usePreProcess==null || !_usePreProcess)
            return null;
        bindingSupport.put(DATA_CONTEXT_BINDING, context);
        return getAttr(PREPROCESS_ATTR).getRealValue();
    }

    private class DataInfo {
        private Object data;
        private DataContext context;

        public DataInfo(Object data, DataContext context) {
            this.data = data;
            this.context = context;
        }
    }
}
