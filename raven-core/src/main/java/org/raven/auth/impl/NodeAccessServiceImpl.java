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

import org.raven.auth.AuthService;
import org.raven.auth.NodeAccessService;
import org.raven.auth.UserContext;
import org.raven.conf.Configurator;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class NodeAccessServiceImpl implements NodeAccessService
{
    private final Tree tree;
    private final Configurator configurator;

    public NodeAccessServiceImpl(Tree tree, Configurator configurator)
    {
        this.tree = tree;
        this.configurator = configurator;
    }

    public int getAccessForNode(Node node, UserContext context)
    {
        String username = context==null? null : context.getUsername();
        return ObjectUtils.equals(AuthService.ROOT_USER_NAME, username)? AccessControl.ADMIN : 0;
    }
}
