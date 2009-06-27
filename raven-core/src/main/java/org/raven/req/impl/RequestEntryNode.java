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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.req.RequestEntry;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RequestHandlerNode.class, importChildTypesFromParent=true)
public class RequestEntryNode extends BaseNode implements RequestEntry
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private RequestEntry nextEntry;

    @NotNull @Parameter(defaultValue="false")
    private Boolean immediateTransition;

    public RequestEntry getNextEntry()
    {
        return nextEntry;
    }

    public void setNextEntry(RequestEntry nextEntry)
    {
        this.nextEntry = nextEntry;
    }

    public Boolean getImmediateTransition() {
        return immediateTransition;
    }

    public void setImmediateTransition(Boolean immediateTransition)
    {
        this.immediateTransition = immediateTransition;
    }
}
