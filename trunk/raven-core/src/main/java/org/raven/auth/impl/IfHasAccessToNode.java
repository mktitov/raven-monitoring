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
package org.raven.auth.impl;

import java.util.Collection;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=LoginListenersNode.class, importChildTypesFromParent=true)
public class IfHasAccessToNode extends BaseNode {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private Node node;
    
	@NotNull @Parameter
	private AccessRight	right;

    @Override
    public boolean isConditionalNode() {
        return true;
    }

    @Override
    public Collection<Node> getEffectiveNodes() {
        if (!isStarted())
            return null;
        int rights = AccessControl.decodeRight(right.getRights());
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        UserContext user = (UserContext) bindings.get(BindingNames.USER_BINDING);
        int nodeRights = user.getAccessForNode(node);
        return nodeRights>=rights? super.getEffectiveNodes() : null;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public AccessRight getRight() {
        return right;
    }

    public void setRight(AccessRight right) {
        this.right = right;
    }
}
