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
package org.raven.expr.impl;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import java.util.Map;
import org.raven.api.NodeAccess;
import org.raven.api.NodeAttributeAccess;

/**
 *
 * @author Mikhail Titov
 */
public class PropertySupport extends GroovyObjectSupport {
    private final Closure action;
    private final NodeAccess node;

    public PropertySupport(Closure action, NodeAccess node) {
        this.action = action;
        this.node = node;
    }
    
    public Object propertyMissing(String name) {
        NodeAttributeAccess attr = node.getAttr(name);
        if (attr!=null)
            return attr.getValue();
        throw new MissingPropertyException(name);
    }
    
    public Object propertyMissing(String name, Object value) {
        NodeAttributeAccess attr = node.getAttr(name);
        if (attr!=null) {
            try {
                attr.setValue(value);
                return attr.getValue();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        throw new MissingPropertyException(name);
    }
    
    public Object methodMissing(String name, Object args) {
        Object[] list = (Object[]) args;
        if (list.length==1 && list[0] instanceof Map) {
            NodeAttributeAccess attr = node.getAt(name);
            if (attr!=null) 
                return attr.getValue((Map)list[0]);
        }
        throw new MissingMethodException(name, action.getClass(), list);
    }   
}
