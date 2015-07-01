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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
public class BasicUserRolesConfiguratorTest extends RavenCoreTestCase {
    
    private BasicUserRolesConfigurator conf;
    
    @Before
    public void prepare() {
        conf = new BasicUserRolesConfigurator();
        conf.setName("roles configurator");
        testsNode.addAndSaveChildren(conf);
    }
    
    @Test
    public void notStartedTest() throws UserContextConfiguratorException {
        UserContextConfig user = createMock(UserContextConfig.class);
        replay(user);
        conf.configure(user);
        verify(user);
    }
    
    @Test
    public void invalidUserTest() throws UserContextConfiguratorException {
        conf.setUsers("user");
        conf.setGroups("group");
        assertTrue(conf.start());
        
        UserContextConfig user = createMock(UserContextConfig.class);
        expect(user.getLogin()).andReturn("user1");
        replay(user);        
        conf.configure(user);
        verify(user);
    }
    
    @Test
    public void validUserTest() throws UserContextConfiguratorException {
        conf.setUsers("user");
        conf.setGroups("group");
        assertTrue(conf.start());
        
        UserContextConfig user = createMock(UserContextConfig.class);
        Set<String> groups = createMock(Set.class);
        expect(user.getLogin()).andReturn("user");
        expect(user.getGroups()).andReturn(groups);
        expect(groups.addAll(eq(new HashSet(Arrays.asList("group"))))).andReturn(true);
        replay(user, groups);        
        conf.configure(user);
        verify(user, groups);
    }
}
