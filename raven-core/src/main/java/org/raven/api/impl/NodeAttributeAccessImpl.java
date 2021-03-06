/*
 *  Copyright 2008 Mikhail Titov.
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

package org.raven.api.impl;

import java.util.Map;
import org.raven.api.NodeAttributeAccess;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAttributeAccessImpl implements NodeAttributeAccess
{
    @Service
    private static TypeConverter converter;

    @Service
    private static Tree tree;

    private final NodeAttribute attribute;

    public NodeAttributeAccessImpl(NodeAttribute attribute) 
    {
        this.attribute = attribute;
    }

    public String getName() {
        return attribute.getName();
    }

    public Object getValue() {
        return attribute.getRealValue();
    }

    public Object getValue(Map args){
        return getAttributeValue(args, false);
    }

    public String getValueAsString() {
        return attribute.getValue();
    }

    public String getValueAsString(Map args){
        return (String)getAttributeValue(args, true);
    }

    private Object getAttributeValue(Map args, boolean asString)
    {
        BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
        boolean initiated = varsSupport.contains(
                ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
        try{
            varsSupport.put(ExpressionAttributeValueHandler.RAVEN_EXPRESSION_ARGS_BINDING, args);
            return asString? attribute.getValue() : attribute.getRealValue();
        }finally{
            if (!initiated)
                varsSupport.reset();
        }
    }

    public void setValue(Object value) throws Exception {
        String strValue = converter.convert(String.class, value, null);
        attribute.setValue(strValue);
        tree.saveNodeAttribute(attribute);
    }

}
