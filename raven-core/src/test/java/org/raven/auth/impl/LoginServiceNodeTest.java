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
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.IllegalLoginException;
import org.raven.auth.IllegalPasswordException;
import org.raven.auth.LoginException;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class LoginServiceNodeTest extends RavenCoreTestCase {
    
    private LoginServiceNode loginService;
    
    @Before
    public void prepare() {
        loginService = new LoginServiceNode();
        loginService.setName("service1");
        tree.getRootNode().addAndSaveChildren(loginService);
        assertTrue(loginService.start());
    }
    
    @Test
    public void structureTest() {
        checkNode(loginService, IpFiltersNode.NAME);
        checkNode(loginService, AuthenticatorsNode.NAME);
        checkNode(loginService, UserContextConfiguratorsNode.NAME);
        checkNode(loginService, ResourcesListNode.NAME);
        checkNode(loginService, GroupsListNode.NAME);
        checkNode(loginService, LoginListenersNode.NAME);
    }
    
    @Test(expected=IllegalLoginException.class)
    public void authNullLoginTest() throws LoginException {
        loginService.login(null, "pwd", "host");
    }
    
    @Test(expected=IllegalLoginException.class)
    public void authEmptyLoginTest() throws LoginException {
        loginService.login(" ", "pwd", "host");
    }
    
    @Test(expected=IllegalPasswordException.class)
    public void authNullPasswordTest() throws LoginException {
        loginService.login("test", null, "host");
    }
    
    @Test(expected=IllegalPasswordException.class)
    public void authEmptyPasswordTest() throws LoginException {
        loginService.login("test", " ", "host");
    }
    
    @Test(expected=AuthenticationFailedException.class)
    public void authFailTest() throws LoginException {
        loginService.login("test", "pwd", "host");
    }
    
    @Test(expected=AuthenticationFailedException.class)
    public void authFail2Test() throws LoginException {
        addAuthenticator(new TestAuthenticator("auth1", false));
        loginService.login("test", "pwd", "host");
    }
    
    @Test
    public void successAuthTest() throws LoginException {
        addAuthenticator(new TestAuthenticator("auth1", true));
        loginService.login("test", "pwd", "host");
    }
    
    @Test
    public void isLoginAllowedFromIp_withoutFilters() {
        assertFalse(loginService.isLoginAllowedFromIp("1.1.1.1"));
    }
    
    @Test
    public void isLoginAllowedFromIp_withFilter() {
        addIpFilter(new AllowAnyIPs(), "allow any");
        assertTrue(loginService.isLoginAllowedFromIp("1.1.1.1"));
    }
    
    private void checkNode(Node owner, String child) {
        Node node = owner.getNode(child);
        assertNotNull(node);
        assertTrue(node.isStarted());
    }
    
    private void addIpFilter(Node filter, String name) {
        filter.setName(name);
        loginService.getIpFiltersNode().addAndSaveChildren(filter);
        assertTrue(filter.start());
    }
    
    private void addAuthenticator(Node authenticator) {
        loginService.getNode(AuthenticatorsNode.NAME).addAndSaveChildren(authenticator);
        assertTrue(authenticator.start());
    }
}
