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

package org.raven.net.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=ParametersNode.class)
public class ParameterNode extends BaseNode
{
    @NotNull @Parameter(defaultValue="java.lang.String")
    private Class parameterType;

    @NotNull @Parameter(defaultValue="true")
    private Boolean required;

    @Parameter
    private String pattern;

    public Class getParameterType()
    {
        return parameterType;
    }

    public void setParameterType(Class parameterType)
    {
        this.parameterType = parameterType;
    }

    public Boolean getRequired()
    {
        return required;
    }

    public void setRequired(Boolean required)
    {
        this.required = required;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }
}
