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

package org.raven.tree.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=ParametersNode.class)
public class ParameterNode extends BaseNode
{
    public static final String PARAMETERVALUE_ATTR = "parameterValue";
    @Parameter()
    @NotNull()
    private String parameterValue;

    @Parameter()
    private Class convertToType;

    @Parameter()
    private String pattern;

    public Class getConvertToType()
    {
        return convertToType;
    }

    public void setConvertToType(Class convertToType)
    {
        this.convertToType = convertToType;
    }

    public String getParameterValue()
    {
        return parameterValue;
    }

    public void setParameterValue(String parameterValue)
    {
        this.parameterValue = parameterValue;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public Object getValue()
    {
        NodeAttribute attr = getNodeAttribute(PARAMETERVALUE_ATTR);
        Class _convertToType = convertToType;
        if (_convertToType==null)
            return attr.getRealValue();
        else
            return converter.convert(convertToType, attr.getRealValue(), pattern);
    }
}
