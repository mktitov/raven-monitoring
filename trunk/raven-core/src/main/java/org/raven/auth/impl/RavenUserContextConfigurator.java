/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.auth.impl;

import java.util.Collection;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextConfigurator;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.SystemNode;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
@Deprecated
public class RavenUserContextConfigurator //implements UserContextConfigurator
{
    private final Tree tree;

    public RavenUserContextConfigurator(Tree tree) {
        this.tree = tree;
    }

//    public void configure(UserContext userContext)
//    {
//        Node configuratorsNode = 
//                tree.getRootNode().getChildren(SystemNode.NAME)
//                .getChildren(AuthorizationNode.NODE_NAME)
//                .getChildren(ContextsNode.NODE_NAME);
//        Collection<UserContextConfigurator> configurators =
//                NodeUtils.getChildsOfType(configuratorsNode, UserContextConfigurator.class);
//        for (UserContextConfigurator configurator: configurators)
//            configurator.configure(userContext);
//    }
}
