/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.impl;

import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.VersionService;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class VersionServiceImplTest extends RavenCoreTestCase {
    
    @Test
    public void serviceTest() {
        VersionService versionService = registry.getService(VersionService.class);
        assertNotNull(versionService);
        Collection<String> versions = versionService.getModulesVersion();
        assertNotNull(versions);
        assertEquals(1, versions.size());
        assertEquals("raven-core: 1.0", versions.iterator().next());
    }
}
