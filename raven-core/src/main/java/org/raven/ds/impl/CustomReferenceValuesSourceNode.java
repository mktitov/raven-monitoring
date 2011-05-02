/*
 *  Copyright 2011 Mikhail Titov.
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
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContextService;
import org.raven.ds.ReferenceValuesSource;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.impl.ReferenceValueImpl;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CustomReferenceValuesSourceNode extends BaseNode implements ReferenceValuesSource
{
    public final static String REFERENCE_VALUES_EXPRESSION_ATTR = "referenceValuesExpression";

    @Service
    private static UserContextService userContextService;

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private String referenceValuesExpression;

    private BindingSupport bindingSupport;

    public String getReferenceValuesExpression() {
        return referenceValuesExpression;
    }

    public void setReferenceValuesExpression(String referenceValuesExpression) {
        this.referenceValuesExpression = referenceValuesExpression;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public List<ReferenceValue> getReferenceValues()
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;

        bindingSupport.put("userContext", userContextService.getUserContext());
        try {
            Object val = getNodeAttribute(REFERENCE_VALUES_EXPRESSION_ATTR).getRealValue();
            if (!(val instanceof Map)) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(
                            "Invalid result value type of the referenceValuesExpression attribute. "
                            + "Expected java.util.Map but was {}"
                            , val==null?"null":val.getClass().getName());
                return null;
            }
            Map<Object, String> values = (Map) val;
            List<ReferenceValue> referenceValues = new ArrayList<ReferenceValue>(values.size());
            for(Map.Entry entry: values.entrySet())
                referenceValues.add(
                    new ReferenceValueImpl(entry.getKey(), entry.getValue().toString()));

            return referenceValues;
            
        } finally {
            bindingSupport.reset();
        }

    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
}
