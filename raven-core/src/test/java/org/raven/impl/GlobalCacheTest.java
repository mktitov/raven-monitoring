/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.impl;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.internal.Cache;
import org.weda.internal.CacheScope;
import org.weda.internal.impl.CacheEntityImpl;
import org.weda.internal.services.CacheManager;

/**
 *
 * @author Mikhail Titov
 */
public class GlobalCacheTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        CacheManager cacheManager = registry.getService(CacheManager.class);
        assertNotNull(cacheManager);
        Cache cache = cacheManager.getCache(CacheScope.GLOBAL);
        assertNotNull(cache);
        cache.put("1", new CacheEntityImpl("test"));
        assertEquals("test", cache.get("1").getValue());
    }
}
