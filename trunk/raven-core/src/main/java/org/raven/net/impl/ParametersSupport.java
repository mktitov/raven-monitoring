/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.net.impl;

import java.util.Map;
import org.raven.net.InvalidParameterValueException;
import org.raven.net.RequiredParameterMissedException;
import org.raven.tree.Node;
import org.raven.util.NodeUtils;
import org.weda.converter.TypeConverterException;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ParametersSupport {
    private final TypeConverter converter;

    public ParametersSupport(TypeConverter converter) {
        this.converter = converter;
    }
    
    public void initNodes(Node owner, boolean start) {
        ContextParameters paramsNode = (ContextParameters) owner.getNode(ContextParameters.NAME);
        if (paramsNode==null) {
             paramsNode = new ContextParameters();
            owner.addAndSaveChildren(paramsNode);
        }
        if (start && paramsNode.isAutoStart())
            paramsNode.start();
        
    }
    
    public ContextParameters getParametersNode(Node owner) {
        return (ContextParameters) owner.getNode(ContextParameters.NAME);
    }
    
    public void checkParameters(Node owner, Map<String, Object> params) 
        throws RequiredParameterMissedException, InvalidParameterValueException
    {
        ContextParameters paramsNode = (ContextParameters) owner.getNode(ContextParameters.NAME);
        if (paramsNode!=null && paramsNode.isStarted()) 
            for (ParameterNode param: NodeUtils.getEffectiveChildsOfType(paramsNode, ParameterNode.class)) {
                Object value = params==null? null : params.get(param.getName());
                if (value!=null) 
                    try {
                        value = converter.convert(param.getParameterType(), value, param.getPattern());
                    } catch (TypeConverterException e) {
                        throw new InvalidParameterValueException(param.getName(), value, param.getParameterType());
                    }
                if (value==null && param.getRequired())
                    throw new RequiredParameterMissedException(param.getName(), owner.getName());
                if (value!=null)
                    params.put(param.getName(), value);
            }
    }
}
