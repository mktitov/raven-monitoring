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
public class MSDomainAuthenticatorTest extends RavenCoreTestCase {
    
    @Test
    public void successWithDefaultDomain() throws AuthenticatorException {
        MSDomainAuthenticator auth = new MSDomainAuthenticator();
        auth.setName("domain auth");
        testsNode.addAndSaveChildren(auth);
        auth.setDomainController(privateProperties.getProperty("domain-controller"));
        auth.setDefaultDomain(privateProperties.getProperty("domain"));
        assertTrue(auth.start());
        assertTrue(auth.checkAuth(privateProperties.getProperty("domain-user"), 
                   privateProperties.getProperty("domain-pwd"), ""));
    }
    
    @Test
    public void successWithDomainInLogin() throws AuthenticatorException {
        MSDomainAuthenticator auth = new MSDomainAuthenticator();
        auth.setName("domain auth");
        testsNode.addAndSaveChildren(auth);
        auth.setDomainController(privateProperties.getProperty("domain-controller"));
        auth.setDefaultDomain("UNKNOWN");
        assertTrue(auth.start());
        assertTrue(auth.checkAuth(
                   privateProperties.getProperty("domain")+"\\"+privateProperties.getProperty("domain-user"), 
                   privateProperties.getProperty("domain-pwd"), ""));
    }
    
    @Test
    public void invalidUser() throws AuthenticatorException {
        MSDomainAuthenticator auth = new MSDomainAuthenticator();
        auth.setName("domain auth");
        testsNode.addAndSaveChildren(auth);
        auth.setDomainController(privateProperties.getProperty("domain-controller"));
        auth.setDefaultDomain(privateProperties.getProperty("domain"));
        assertTrue(auth.start());
        assertFalse(auth.checkAuth(
                   "unknown-user", 
                   privateProperties.getProperty("domain-pwd"), ""));
    }
}
