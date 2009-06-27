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

package org.raven.req.impl;

import java.util.Collection;
import java.util.Map;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.IfNode;
import org.raven.req.RequestEntry;
import org.raven.req.RequestHandler;
import org.raven.req.RequestHandlerException;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=InvisibleNode.class,
    childNodes={IfNode.class, RequestEntryNode.class},
    importChildTypesFromParent=true)
public class RequestHandlerNode extends BaseNode implements RequestHandler
{
    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    public Collection<Node> handleRequest(Map<String, Object> params) throws RequestHandlerException
    {
        Node currentPosition = this;
        RequestEntry nextEntry = (RequestEntry) params.get(NEXT_ENTRY_PARAM);
        if (nextEntry!=null)
        {
            currentPosition = nextEntry.getNextEntry();
            if (currentPosition==null)
                currentPosition = nextEntry;
        }

        for (Map.Entry<String, Object> paramEntry: params.entrySet())
            bindingSupport.put(paramEntry.getKey(), paramEntry.getValue());
        try
        {
            Collection<Node> actions = currentPosition.getEffectiveChildrens();
            if (actions!=null && !actions.isEmpty())
                for (Node action: actions)
                    if (action instanceof RequestEntry)
                        params.put(NEXT_ENTRY_PARAM, action);
                
            return actions;
        }
        finally
        {
            bindingSupport.reset();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

}
