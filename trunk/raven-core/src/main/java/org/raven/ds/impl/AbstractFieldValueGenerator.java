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

import javax.script.Bindings;
import org.raven.ds.DataContext;
import org.raven.ds.FieldValueGenerator;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractFieldValueGenerator extends BaseNode implements FieldValueGenerator
{
    protected BindingSupportImpl bindingSupport;

    @Override
    protected void initFields() {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    

    public Object getFieldValue(DataContext context)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Forming field value");
        bindingSupport.put(SESSION_ATTRIBUTES_BINDING, context.getSessionAttributes());
        try
        {
            Object val = doGetFieldValue(context);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Field value formed - (%s)", val));
            return val;
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    protected abstract Object doGetFieldValue(DataContext context);
}
