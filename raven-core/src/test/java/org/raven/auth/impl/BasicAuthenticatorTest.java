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

import static org.junit.Assert.*;
import org.junit.Test;
import org.raven.auth.AuthenticatorException;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class BasicAuthenticatorTest extends RavenCoreTestCase 
{
    @Test
    public void test() throws AuthenticatorException {
        BasicAuthenticator auth = new BasicAuthenticator();
        auth.setName("testLogin");
        testsNode.addAndSaveChildren(auth);
        auth.setPassword("testPwd");
        
        assertFalse(auth.checkAuth("testLogin", "testPwd"));
        
        assertTrue(auth.start());        
        assertTrue(auth.checkAuth("testLogin", "testPwd"));
        assertFalse(auth.checkAuth("testLogin1", "testPwd"));
        assertFalse(auth.checkAuth("testLogin", "testPwd1"));
    }
}
