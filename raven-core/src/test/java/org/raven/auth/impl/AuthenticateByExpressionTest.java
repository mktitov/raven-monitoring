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

import javax.script.Bindings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.auth.AuthenticatorException;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class AuthenticateByExpressionTest extends RavenCoreTestCase {
    
    private AuthenticateByExpression auth;
    
    @Before
    public void prepare() throws Exception {
        auth = new AuthenticateByExpression();
        auth.setName("expr auth");
        testsNode.addAndSaveChildren(auth);
        auth.getAttr(AuthenticateByExpression.AUTHENTICATE_ATTR).setValue("login=='test' && password=='pwd'");
    }
    
    @Test
    public void notStartedTest() throws AuthenticatorException {
        assertFalse(auth.checkAuth("test", "pwd", ""));
    }
    
    @Test
    public void invalidUserTest() throws AuthenticatorException {
        assertTrue(auth.start());
        assertFalse(auth.checkAuth("test1", "pwd", ""));
    }
    
    @Test
    public void invalidPwdTest() throws AuthenticatorException {
        assertTrue(auth.start());
        assertFalse(auth.checkAuth("test", "pwd1", ""));
    }
    
    @Test
    public void successTest() throws AuthenticatorException {
        assertTrue(auth.start());
        assertTrue(auth.checkAuth("test", "pwd", ""));
    }
}
