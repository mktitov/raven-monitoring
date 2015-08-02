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

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.IllegalLoginException;
import org.raven.auth.IllegalPasswordException;
import org.raven.auth.InvalidLoginUrlException;
import org.raven.auth.LoginException;
import org.raven.auth.LoginPathChecker;
import org.raven.net.ResponseContext;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeError;

/**
 *
 * @author Mikhail Titov
 */
@RunWith(JMockit.class)
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
        checkNode(loginService, LoginPathsNode.NAME);
        checkNode(loginService, AuthenticatorsNode.NAME);
        checkNode(loginService, UserContextConfiguratorsNode.NAME);
        checkNode(loginService, ResourcesListNode.NAME);
        checkNode(loginService, GroupsListNode.NAME);
        checkNode(loginService, LoginListenersNode.NAME);
    }
    
    @Test(expected=IllegalLoginException.class)
    public void authNullLoginTest() throws LoginException {
        loginService.login(null, "pwd", "host", null);
    }
    
    @Test(expected=IllegalLoginException.class)
    public void authEmptyLoginTest() throws LoginException {
        loginService.login(" ", "pwd", "host", null);
    }
    
    @Test(expected=IllegalPasswordException.class)
    public void authNullPasswordTest() throws LoginException {
        loginService.login("test", null, "host", null);
    }
    
    @Test(expected=IllegalPasswordException.class)
    public void authEmptyPasswordTest() throws LoginException {
        loginService.login("test", " ", "host", null);
    }
    
    @Test(expected=AuthenticationFailedException.class)
    public void authFailTest() throws LoginException {
        loginService.login("test", "pwd", "host", null);
    }
    
    @Test(expected=AuthenticationFailedException.class)
    public void authFail2Test() throws LoginException {
        addAuthenticator(new TestAuthenticator("auth1", false));
        loginService.login("test", "pwd", "host", null);
    }
    
    @Test
    public void successAuthTest() throws LoginException {
        addAuthenticator(new TestAuthenticator("auth1", true));
        loginService.login("test", "pwd", "host", null);
    }
    
    @Test
    public void isLoginAllowedFromIp_withoutFilters() {
        assertTrue(loginService.isLoginAllowedFromIp("1.1.1.1"));
    }
    
    @Test
    public void isLoginAllowedFromIp_withFilter() {
        addIpFilter(new AllowAnyIPs(), "allow any");
        assertTrue(loginService.isLoginAllowedFromIp("1.1.1.1"));
    }
    
    @Test(expected = InvalidLoginUrlException.class)
    public void loginPathCheck(
            final @Mocked ResponseContext responseContext,
            final @Mocked LoginPathChecker checker
    ) throws Exception 
    {
        new Expectations() {{
            checker.isLoginAllowedFromPath(responseContext); result = false;
        }};
        addLoginPathChecker(checker);
        loginService.login("test", "pwd", "host", responseContext);
    }

    private void addLoginPathChecker(final LoginPathChecker checker) throws NodeError {
        TestLoginPathChecker checkerNode = new TestLoginPathChecker();
        checkerNode.setName("test checker");
        loginService.getNode(LoginPathsNode.NAME).addAndSaveChildren(checkerNode);
        checkerNode.setChecker(checker);
        assertTrue(checkerNode.start());
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
