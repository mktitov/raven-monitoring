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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Bindings;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
public class RootNode extends BaseNode
{
    private Map<String, BindingSupport> bindingSupports;

    public RootNode()
    {
        super("");
        bindingSupports = new ConcurrentHashMap<String, BindingSupport>();
    }

    public void addBindingSupport(String bindingSupportId, BindingSupport bindingSupport)
    {
        bindingSupports.put(bindingSupportId, bindingSupport);
    }

    public void removeBindingSupport(String bindingSupportId)
    {
        bindingSupports.remove(bindingSupportId);
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);

        for (BindingSupport bindingSupport: bindingSupports.values())
            bindingSupport.addTo(bindings);
    }
}
