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

package org.raven.tree.impl;

import java.util.Arrays;
import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 * The node that references to other node
 * @author Mikhail Titov
 */
@NodeClass
public class ReferenceNode extends BaseNode
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private Node reference;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean useInTemplate;

    @Override
    public boolean isConditionalNode()
    {
        return (isTemplate() && useInTemplate) || (!isTemplate() && !useInTemplate);
    }

    @Override
    public Collection<Node> getEffectiveChildrens()
    {
        if (!isConditionalNode())
            return null;
        Node refNode = reference;
        if (refNode==null)
            return null;
        else if (refNode.isConditionalNode())
            return refNode.getEffectiveChildrens();
        else
            return Arrays.asList(refNode);
    }

    public Node getReference()
    {
        return reference;
    }

    public void setReference(Node reference)
    {
        this.reference = reference;
    }

    public Boolean getUseInTemplate()
    {
        return useInTemplate;
    }

    public void setUseInTemplate(Boolean useInTemplate)
    {
        this.useInTemplate = useInTemplate;
    }

}
