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

import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordGeneratorNode.class)
public class AttributeRecordFieldValueGenerator
        extends AbstractFieldValueGenerator
{
    public static final String VALUE_ATTRIBUTE = "value";
    @Parameter
    private String value;

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    protected Object doGetFieldValue(Map<String, NodeAttribute> sessionAttributes)
    {
        return getNodeAttribute(VALUE_ATTRIBUTE).getRealValue();
    }
}
