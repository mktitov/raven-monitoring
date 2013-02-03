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

import java.util.HashMap;
import java.util.Map;
import org.raven.net.RequiredParameterMissedException;
import org.raven.tree.Node;
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
    
    public void initNodes(Node owner) {
        if (owner.hasNode(ContextParameters.NAME))
            return;
        ContextParameters paramsNode = new ContextParameters();
        owner.addAndSaveChildren(paramsNode);
        paramsNode.start();
    }
    
    public Map<String, Object> checkParameters(Node owner, Map<String, Object> params) 
        throws RequiredParameterMissedException 
    {
        ContextParameters paramsNode = (ContextParameters) owner.getNode(ContextParameters.NAME);
        if (paramsNode==null)
            return params;
        Map<String, Object> newParams = new HashMap<String, Object>();
        if (params!=null)
            newParams.putAll(params);
        for (Node node: paramsNode.getEffectiveNodes())
            if (node.isStarted() && node instanceof ParameterNode) {
                ParameterNode param = (ParameterNode) node;
                Object value = params==null? null : params.get(param.getName());
                if (value!=null)
                    value = converter.convert(param.getParameterType(), value, param.getPattern());
                if (value==null && param.getRequired())
                    throw new RequiredParameterMissedException(param.getName(), owner.getName());
                if (value!=null)
                    newParams.put(param.getName(), value);
            }
        return newParams;
    }
}
