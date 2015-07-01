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

import org.weda.internal.Cache;
import org.weda.internal.CacheEntity;
import org.weda.internal.CacheScope;
import org.weda.internal.impl.CacheEntityImpl;
import org.weda.internal.services.CacheManager;

/**
 *
 * @author Mikhail Titov
 */
public class TemporaryCacheManagerImpl implements TemporaryCacheManager
{
    private final CacheManager cacheManager;

    public TemporaryCacheManagerImpl(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public TemporaryCache getCache(CacheScope scope)
    {
        Cache cache = cacheManager.getCache(scope);
        synchronized(cache)
        {
            CacheEntity<TemporaryCache> entity = cache.get(TEMPORARY_CACHE_KEY);
            if (entity==null)
            {
                entity = new CacheEntityImpl(new TemporaryCache());
                cache.put(TEMPORARY_CACHE_KEY, entity);
            }
            return entity.getValue();
        }
    }
}
