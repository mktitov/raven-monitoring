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
package org.raven.expr.impl;

import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
public class BindingSupportImpl implements BindingSupport {

    ThreadLocal<Map<String, Object>> bindings = new ThreadLocal<Map<String, Object>>();
    private boolean forceDisableScriptExcecution = false;

    public void setForceDisableScriptExcecution(boolean value)
    {
        forceDisableScriptExcecution = value;
    }

    public void enableScriptExecution() {
        put(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING, true);
    }

    public void put(String bindingName, Object value) {
        Map<String, Object> binds = bindings.get();
        if (binds == null) {
            binds = new HashMap<String, Object>();
            if (!forceDisableScriptExcecution)
                binds.put(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING, true);
            bindings.set(binds);
        }
        binds.put(bindingName, value);
    }

    public void putAll(Bindings bindMap) {
        Map<String, Object> binds = bindings.get();
        if (binds == null) {
            binds = new HashMap<String, Object>();
            if (!forceDisableScriptExcecution)
                binds.put(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING, true);
            bindings.set(binds);
        }
        binds.putAll(bindMap);
    }

    public void reset() {
        bindings.remove();
    }

    public void addTo(Bindings scriptBindings) {
        if (bindings.get() != null) {
            scriptBindings.putAll(bindings.get());
        }
    }

    public Object get(String bindingName)
    {
        if (bindings.get()!=null)
            return bindings.get().get(bindingName);
        else
            return null;
    }

    public Object remove(String bindingName) {
        if (bindings.get()!=null)
            return bindings.get().remove(bindingName);
        else
            return null;
    }

    public boolean contains(String bindingName)
    {
        if (bindings.get()!=null)
            return bindings.get().containsKey(bindingName);
        else
            return false;
    }
}
