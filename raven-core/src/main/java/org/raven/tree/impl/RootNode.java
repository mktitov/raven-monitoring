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
import org.raven.annotations.NodeClass;
import org.raven.expr.BindingSupport;
import org.raven.tree.Tree;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, anyChildTypes=true)
public class RootNode extends BaseNode
{
    private Map<String, BindingSupport> bindingSupports;

    public RootNode() {
        super("");
        bindingSupports = new ConcurrentHashMap<String, BindingSupport>();
    }

    /**
     * @see Tree#addGlobalBindings(String, org.raven.expr.BindingSupport) 
     */
    public void addBindingSupport(String bindingSupportId, BindingSupport bindingSupport) {
        if (bindingSupports.containsKey(bindingSupportId))
            return;
        bindingSupport.setForceDisableScriptExcecution(true);
        bindingSupports.put(bindingSupportId, bindingSupport);
    }

    /**
     * @see Tree#getGlobalBindings(String)
     */
    public BindingSupport getBindingSupport(String bindingSupportId) {
        return bindingSupports.get(bindingSupportId);
    }

    /**
     * @see Tree#removeGlobalBindings(String) 
     */
    public void removeBindingSupport(String bindingSupportId) {
        BindingSupport bindingSupport = bindingSupports.remove(bindingSupportId);
        bindingSupport.setForceDisableScriptExcecution(false);
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        for (BindingSupport bindingSupport: bindingSupports.values())
            bindingSupport.addTo(bindings);
    }
}
