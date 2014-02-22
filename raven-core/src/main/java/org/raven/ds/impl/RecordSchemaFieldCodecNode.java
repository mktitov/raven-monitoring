/*
 * Copyright 2014 Mikhail Titov.
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

import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.RecordSchemaFieldCodec;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = InvisibleNode.class)
public class RecordSchemaFieldCodecNode extends BaseNode implements RecordSchemaFieldCodec {
    
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Object encodeExpression;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useEncodeExpression;

    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Object decodeExpression;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useDecodeExpression;
    
    private BindingSupport bindingsSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingsSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingsSupport.addTo(bindings);
    }

    public <T> T encode(Object value, Bindings bindings) {
        if (!useEncodeExpression) 
            return (T)value;
        bindingsSupport.put(BindingNames.VALUE_BINDING, value);
        if (bindings!=null && !bindings.isEmpty())
            bindingsSupport.putAll(bindings);
        try {
            return (T)encodeExpression;
        } finally {
            bindingsSupport.reset();
        }
    }

    public <T> T decode(Object value, Bindings bindings) {
        if (!useDecodeExpression) 
            return (T)value;
        bindingsSupport.put(BindingNames.VALUE_BINDING, value);
        if (bindings!=null && !bindings.isEmpty())
            bindingsSupport.putAll(bindings);
        try {
            return (T)decodeExpression;
        } finally {
            bindingsSupport.reset();
        }
    }

    public Object getEncodeExpression() {
        return encodeExpression;
    }

    public void setEncodeExpression(Object encodeExpression) {
        this.encodeExpression = encodeExpression;
    }

    public Boolean isUseEncodeExpression() {
        return useEncodeExpression;
    }

    public void setUseEncodeExpression(Boolean useEncodeExpression) {
        this.useEncodeExpression = useEncodeExpression;
    }

    public Object getDecodeExpression() {
        return decodeExpression;
    }

    public void setDecodeExpression(Object decodeExpression) {
        this.decodeExpression = decodeExpression;
    }

    public Boolean isUseDecodeExpression() {
        return useDecodeExpression;
    }

    public void setUseDecodeExpression(Boolean useDecodeExpression) {
        this.useDecodeExpression = useDecodeExpression;
    }
}
