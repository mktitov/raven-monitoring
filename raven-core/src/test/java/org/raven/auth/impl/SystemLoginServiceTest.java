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

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.raven.auth.LoginException;
import org.raven.auth.UserContext;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class SystemLoginServiceTest extends RavenCoreTestCase {
    private SystemLoginService service;
    
    @Before
    public void prepare() {
        ContainerNode container = new ContainerNode("container");
        tree.getRootNode().addAndSaveChildren(container);
        assertTrue(container.start());
        service = new SystemLoginService();
        container.addAndSaveChildren(service);
        assertTrue(service.start());
    }
    
    @Test
    public void structureTest() {
        Node rootAuth = service.getAuthenticatorsNode().getNode(RootUserAuthenticator.NAME);
        assertNotNull(rootAuth);
        assertTrue(rootAuth instanceof RootUserAuthenticator);
        assertStarted(rootAuth);
        
        Node adminsConfig = service.getUserContextConfiguratorsNode().getNode(SystemLoginService.ADMINISTRATORS);
        assertNotNull(adminsConfig);
        assertTrue(adminsConfig instanceof AdminsConfigurator);
        assertEquals(RootUserAuthenticator.ROOT_USER_NAME, ((AdminsConfigurator)adminsConfig).getUsers());
        
        Node allowAnyFilter = service.getIpFiltersNode().getNode(SystemLoginService.ALLOW_ANY_FILTER);
        assertNotNull(allowAnyFilter);
        assertTrue(allowAnyFilter instanceof AllowAnyIPs);
    }
    
    @Test
    public void rootLoginTest() throws LoginException {
        assertTrue(service.isLoginAllowedFromIp("1.1.1.1"));
        UserContext user = service.login(RootUserAuthenticator.ROOT_USER_NAME, "12345", "host", null);
        assertNotNull(user);
        assertEquals(RootUserAuthenticator.ROOT_USER_NAME, user.getLogin());
        assertTrue(user.isAdmin());
    }
}
