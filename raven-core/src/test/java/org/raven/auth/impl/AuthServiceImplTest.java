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

import org.raven.conf.Configurator;
import java.util.Arrays;
import java.util.Collection;
import org.raven.auth.UserContext;
import org.junit.Test;
import org.raven.auth.AuthProvider;
import org.raven.auth.AuthService;
import org.raven.conf.Config;
import org.raven.test.RavenCoreTestCase;
import org.slf4j.LoggerFactory;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class AuthServiceImplTest extends RavenCoreTestCase
{
    @Test
    public void instanceAuthenticateTest() throws Exception
    {
        AuthProvider provider1 = createMock("provider1", AuthProvider.class);
        AuthProvider provider2 = createMock("provider2", AuthProvider.class);
        AuthProvider provider3 = createMock("provider3", AuthProvider.class);
        Configurator cfgr = createMock("configurator", Configurator.class);
        Config config  = createMock("config", Config.class);

        expect(provider1.authenticate("user1", "pass1")).andReturn(null);
        expect(provider2.authenticate("user1", "pass1")).andReturn("provider2");
        expect(cfgr.getConfig()).andReturn(config).times(2);
        expect(config.getStringProperty(Configurator.AUTH_ROOT_PASSWORD, null)).andReturn("12345").times(2);

        replay(provider1, provider2, provider3, cfgr, config);

        Collection<AuthProvider> providers = Arrays.asList(provider1, provider2);

        AuthServiceImpl authService = new AuthServiceImpl(
                providers, cfgr, LoggerFactory.getLogger(AuthService.class));
        UserContext context = authService.authenticate("user1", "pass1");
        assertEquals("user1", context.getUsername());
        assertEquals("provider2", context.getAuthProvider());

        assertNull(authService.authenticate("root", "321"));
        context = authService.authenticate("root", "12345");
        assertNotNull(context);
        assertEquals("root", context.getUsername());
        assertEquals("root", context.getAuthProvider());
        assertTrue(context.isAdmin());

        verify(provider1, provider2, provider3, cfgr, config);
    }

    @Test
    public void serviceTest()
    {
        AuthService service = registry.getService(AuthService.class);
        assertNotNull(service);
        assertNull(service.authenticate("unknownUser", "pass"));

        UserContext context = service.authenticate("root", "12345");
        assertNotNull(context);
        assertEquals("root", context.getUsername());
        assertEquals("root", context.getAuthProvider());
        assertTrue(context.isAdmin());

        assertNull(service.authenticate("root", "123"));
    }
}