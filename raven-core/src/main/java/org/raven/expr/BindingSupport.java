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

package org.raven.expr;

import java.util.Map;
import javax.script.Bindings;

/**
 *
 * @author Mikhail Titov
 */
public interface BindingSupport
{
    public void setForceDisableScriptExcecution(boolean value);
    public void enableScriptExecution();
    public void put(String bindingName, Object value);
    public void putAll(Map<String, Object> bindMap);
    public void addTo(Bindings scriptBindings);
    public Object get(String bindingName);
    public boolean contains(String bindingName);
    public Object remove(String bindingName);
    public void reset();
}
