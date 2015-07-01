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

package org.raven.rest.beans;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeBean
{
    public String name;
    public String displayName;
    public String type;
    public String description;
    public String value;
    public String parentAttribute;
    public String valueHandlerType;
    public boolean builtIn;
    public boolean required;
    public boolean expression;
    public boolean templateExpression;
    public boolean readOnly;

    public NodeAttributeBean() {
    }

    public NodeAttributeBean(
            String name, String displayName, String type, String description
            , String value, String parentAttribute, String valueHandlerType, boolean builtIn
            , boolean required, boolean expression, boolean templateExpression, boolean readOnly)
    {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.description = description;
        this.value = value;
        this.parentAttribute = parentAttribute;
        this.valueHandlerType = valueHandlerType;
        this.builtIn = builtIn;
        this.required = required;
        this.expression = expression;
        this.templateExpression = templateExpression;
        this.readOnly = readOnly;
    }
}
