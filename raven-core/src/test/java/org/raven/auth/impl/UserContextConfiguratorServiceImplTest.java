/*
 *  Copyright 2010 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.auth.impl;

import java.util.Arrays;
import org.junit.Test;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextConfigurator;
import org.raven.auth.UserContextConfiguratorService;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class UserContextConfiguratorServiceImplTest extends RavenCoreTestCase
{
    @Test
    public void instanceTest()
    {
        UserContextConfigurator conf = createMock(UserContextConfigurator.class);
        UserContext context = createMock(UserContext.class);
        conf.configure(eq(context));

        replay(conf);

        UserContextConfiguratorServiceImpl service =
                new UserContextConfiguratorServiceImpl(Arrays.asList(conf));
        service.configure(context);

        verify(conf);
    }

    @Test
    public void serviceTest()
    {
        UserContextConfiguratorService serv = registry.getService(UserContextConfiguratorService.class);
        assertNotNull(serv);
        serv.configure(new UserContextImpl("test", "test"));
    }
}