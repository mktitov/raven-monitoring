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

import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.auth.UserContextConfig;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
import org.raven.auth.UserContextConfiguratorException;

/**
 *
 * @author Mikhail Titov
 */
public class DataConsumerUserConfiguratorTest extends RavenCoreTestCase {
    private DataConsumerUserConfigurator conf;
    private PushOnDemandDataSource ds;
    
    @Before
    public void prepare() {
        ds = new PushOnDemandDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        conf = new DataConsumerUserConfigurator();
        conf.setName("configurator");
        testsNode.addAndSaveChildren(conf);
        conf.setDataSource(ds);
    }
    
    @Test
    public void notStartedTest() throws UserContextConfiguratorException {
        UserContextConfig user = createMock(UserContextConfig.class);
        replay(user);
        conf.configure(user);
        verify(user);
    }
    
    @Test
    public void successConfigureTest() throws UserContextConfiguratorException {
        UserContextConfig user = createMock(UserContextConfig.class);
        Set<String> groups = createMock(Set.class);
        expect(user.getLogin()).andReturn("test");
        expect(user.getGroups()).andReturn(groups);
        expect(groups.add("test-group")).andReturn(true);
        replay(user);
        
        conf.setBeforeConfigure("context['group']=user.login");
        conf.setConfigure("user.groups.add(context['group']+'-'+data[0])");
        assertTrue(conf.start());
        ds.addDataPortion("group");
        conf.configure(user);
        verify(user);
    }
}
