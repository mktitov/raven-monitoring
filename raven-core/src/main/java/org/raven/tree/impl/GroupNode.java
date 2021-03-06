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

import java.util.Collection;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, importChildTypesFromParent=true)
public class GroupNode extends BaseNode
{
    private ThreadLocal<Node> effectiveParent;

    @Override
    protected void initFields() {
        super.initFields();
        effectiveParent = new ThreadLocal<Node>();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        Node newParent = effectiveParent.get();
        if (newParent==null) super.formExpressionBindings(bindings);
        else newParent.formExpressionBindings(bindings);
    }

    @Override
    public Node getEffectiveParent() {
        Node newParent = effectiveParent.get();
        return newParent==null? super.getEffectiveParent() : newParent.getEffectiveParent();
    }
    
    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        return !isStarted()? null : super.getEffectiveNodes();
    }
    
    public Collection<Node> getEffectiveNodes(Node effectiveParent) {
        this.effectiveParent.set(effectiveParent);
        try {
            return getEffectiveNodes();
        } finally {
            this.effectiveParent.remove();
        }
    }
}