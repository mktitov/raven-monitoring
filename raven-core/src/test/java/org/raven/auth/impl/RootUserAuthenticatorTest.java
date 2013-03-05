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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.auth.AuthenticatorException;
import org.raven.auth.UserContextConfig;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class RootUserAuthenticatorTest extends RavenCoreTestCase {
    private RootUserAuthenticator rootAuth;
    
    @Before
    public void prepare() {
        rootAuth = new RootUserAuthenticator();
        tree.getRootNode().addAndSaveChildren(rootAuth);
        assertTrue(rootAuth.start());
    }
    
    @Test
    public void notRootTest() throws AuthenticatorException {
        assertFalse(rootAuth.checkAuth(user("test"), "test"));
    }
    
    @Test
    public void invalidPasswordTest() throws AuthenticatorException {
        assertFalse(rootAuth.checkAuth(user("root"), "test"));
    }
    
    @Test
    public void validAuthTest() throws AuthenticatorException {
        assertTrue(rootAuth.checkAuth(user("root"), "12345"));
    }
    
    public static UserContextConfig user(String login) {
        return new UserContextConfigImpl(login, "");
    }
}
