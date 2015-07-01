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
import org.raven.auth.LoginException;
import org.raven.auth.LoginManager;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class LoginManagerServiceTest extends RavenCoreTestCase {
    
    @Test(expected=LoginException.class)
    public void authServiceNotFoundTest() throws LoginException {
        LoginManager manager = registry.getService(LoginManager.class);
        assertNotNull(manager);
        manager.getLoginService("service1");
    }
    
    @Test(expected=LoginException.class)
    public void authServiceUnavailableTest() throws LoginException {
        LoginManagerNode managerNode = (LoginManagerNode) 
                tree.getRootNode()
                .getNode(SystemNode.NAME)
                .getNode(AuthorizationNode.NODE_NAME)
                .getNode(LoginManagerNode.NAME);
        assertNotNull(managerNode);
        
        TestAuthService authService = new TestAuthService("service1");
        managerNode.addAndSaveChildren(authService);
        
        LoginManager manager = registry.getService(LoginManager.class);
        assertNotNull(manager);
        manager.getLoginService("service1");
    }
    
    @Test
    public void test() throws LoginException {
        LoginManagerNode managerNode = (LoginManagerNode) 
                tree.getRootNode()
                .getNode(SystemNode.NAME)
                .getNode(AuthorizationNode.NODE_NAME)
                .getNode(LoginManagerNode.NAME);
        assertNotNull(managerNode);
        
        TestAuthService authService = new TestAuthService("service1");
        managerNode.addAndSaveChildren(authService);
        assertTrue(authService.start());
        
        LoginManager manager = registry.getService(LoginManager.class);
        assertNotNull(manager);
        assertSame(authService, manager.getLoginService("service1"));
    }
    
}
