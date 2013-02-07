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
import org.raven.auth.UserContextConfig;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
import org.raven.auth.UserContextConfiguratorException;

/**
 *
 * @author Mikhail Titov
 */
public class AdminsConfiguratorTest extends RavenCoreTestCase {
    private AdminsConfigurator configurator;
    
    @Before
    public void prepare() {
        configurator = new AdminsConfigurator();
        configurator.setName("Administrators");
        tree.getRootNode().addAndSaveChildren(configurator);
    }
    
    @Test
    public void nullUsersTest() throws UserContextConfiguratorException {
        assertTrue(configurator.start());
        UserContextConfig config = createUserContextMock();
        replay(config);
        configurator.configure(config);
        verify(config);
    }
    
    @Test
    public void invalidUserTest() throws UserContextConfiguratorException {
        configurator.setUsers("admin");
        assertTrue(configurator.start());
        UserContextConfig config = createUserContextMock();
        replay(config);
        configurator.configure(config);
        verify(config);
    }
    
    @Test
    public void validUserTest() throws UserContextConfiguratorException {
        configurator.setUsers("test");
        assertTrue(configurator.start());
        UserContextConfig config = createUserContextMock();
        config.setAdmin(true);
        replay(config);
        configurator.configure(config);
        verify(config);
    }
    
    @Test
    public void validUserInListTest() throws UserContextConfiguratorException {
        configurator.setUsers("admin, test");
        assertTrue(configurator.start());
        UserContextConfig config = createUserContextMock();
        config.setAdmin(true);
        replay(config);
        configurator.configure(config);
        verify(config);
    }
    
    private UserContextConfig createUserContextMock() {
        UserContextConfig config = createMock(UserContextConfig.class);
        expect(config.getLogin()).andReturn("test");
        return config;
    }
}
