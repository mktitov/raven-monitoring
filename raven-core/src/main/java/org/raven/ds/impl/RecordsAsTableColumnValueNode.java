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
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.util.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordsAsTableNode.class)
public class RecordsAsTableColumnValueNode extends BaseNode
{
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
//    @NotNull
    private String columnValue;

    @Parameter
    @NotNull
    private Integer columnNumber;

    private BindingSupport bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();

        bindingSupport = new BindingSupport();
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        bindingSupport.addTo(bindings);
    }

    public void addBinding(String name, Object value)
    {
        bindingSupport.put(name, value);
    }

    public void resetBindings()
    {
        bindingSupport.reset();
    }

    public Integer getColumnNumber()
    {
        return columnNumber;
    }

    public void setColumnNumber(Integer columnNumber)
    {
        this.columnNumber = columnNumber;
    }

    public String getColumnValue()
    {
        return columnValue;
    }

    public void setColumnValue(String columnValue)
    {
        this.columnValue = columnValue;
    }
}
