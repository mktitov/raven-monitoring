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

/**
 *
 * @author Mikhail Titov
 */
public class SystemLoginService extends LoginServiceNode {
    public final static String NAME = "System";
    public final static String ADMINISTRATORS = "Administrators";
    public final static String ALLOW_ANY_FILTER = "Allow any";

    public SystemLoginService() {
        super(NAME);
    }

    @Override
    protected void initChildren() {
        super.initChildren();
        IpFiltersNode filtersNode = getIpFiltersNode();
        if (!filtersNode.hasNode(ALLOW_ANY_FILTER)) {
            AllowAnyIPs allowAny = new AllowAnyIPs();
            allowAny.setName(ALLOW_ANY_FILTER);
            filtersNode.addAndSaveChildren(allowAny);
            allowAny.start();
        }
        AuthenticatorsNode authsNode = getAuthenticatorsNode();
        if (!authsNode.hasNode(RootUserAuthenticator.NAME)) {
            RootUserAuthenticator node = new RootUserAuthenticator();
            authsNode.addAndSaveChildren(node);
            node.start();
        }
        UserContextConfiguratorsNode configuratorsNode = getUserContextConfiguratorsNode();
        if (!configuratorsNode.hasNode(ADMINISTRATORS)) {
            AdminsConfigurator node = new AdminsConfigurator();
            node.setName(ADMINISTRATORS);
            configuratorsNode.addAndSaveChildren(node);
            node.setUsers(RootUserAuthenticator.ROOT_USER_NAME);
            node.start();
        }
    }
}
