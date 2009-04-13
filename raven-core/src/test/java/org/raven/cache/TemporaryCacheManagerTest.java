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

package org.raven.cache;

import org.junit.Test;
import org.raven.RavenCoreTestCase;
import org.weda.internal.Cache;
import org.weda.internal.CacheEntity;
import org.weda.internal.CacheScope;
import org.weda.internal.services.CacheManager;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class TemporaryCacheManagerTest extends RavenCoreTestCase
{
    @Test
    public void implementationTest()
    {
        TemporaryCache tempCache1 = new TemporaryCache();
        TemporaryCache tempCache2 = new TemporaryCache();

        CacheManager cacheManager = createMock(CacheManager.class);
        Cache cache1 = createMock("cache1", Cache.class);
        Cache cache2 = createMock("cache2", Cache.class);
        CacheEntity entity1 = createMock("entity1", CacheEntity.class);
        CacheEntity entity2 = createMock("entity1", CacheEntity.class);
        expect(cacheManager.getCache(CacheScope.GLOBAL)).andReturn(cache1);
        expect(cache1.get(TemporaryCacheManager.TEMPORARY_CACHE_KEY)).andReturn(entity1);
        expect(entity1.getValue()).andReturn(tempCache1);
        expect(cacheManager.getCache(CacheScope.SESSION)).andReturn(cache2);
        expect(cache2.get(TemporaryCacheManager.TEMPORARY_CACHE_KEY)).andReturn(entity2);
        expect(entity2.getValue()).andReturn(tempCache2);

        replay(cacheManager, cache1, cache2, entity1, entity2);

        TemporaryCacheManager tempCacheManager = new TemporaryCacheManagerImpl(cacheManager);
        assertSame(tempCache1, tempCacheManager.getCache(CacheScope.GLOBAL));
        assertSame(tempCache2, tempCacheManager.getCache(CacheScope.SESSION));

        verify(cacheManager, cache1, cache2, entity1, entity2);
    }

    @Test
    public void serviceTest()
    {
        TemporaryCacheManager temporaryCacheManager =
                registry.getService(TemporaryCacheManager.class);
        assertNotNull(temporaryCacheManager);
        assertNotNull(temporaryCacheManager.getCache(CacheScope.GLOBAL));
    }
}