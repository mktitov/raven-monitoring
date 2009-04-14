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

package org.raven.ds.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.cache.TemporaryCache;
import org.raven.cache.TemporaryCacheManager;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.ds.DataStore;
import org.raven.ds.DataStoreException;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.Cache;
import org.weda.internal.CacheEntity;
import org.weda.internal.CacheScope;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.CacheManager;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class CachePipeNode extends AbstractDataPipe
{
    public static final String CONSUMER_TYPE_ATTR = "activeConsumer";
    public static final String PREPARING_FLAG_ATTR = "CachePipeNode_DataPreparingFlag";
    public static final String DATA_STORE_ATTR = "CachePipeNode_DataStore";
    
    @Service
    protected static CacheManager cacheManager;
    @Service
    protected static TemporaryCacheManager temporaryCacheManager;
    @Service
    protected static MessagesRegistry messages;

    @Parameter(defaultValue="SESSION")
    @NotNull
    private CacheScope cacheScope;

    @Parameter(defaultValue="1000")
    @NotNull
    private Long waitTimeout;

    @Parameter(defaultValue="5000")
    @NotNull
    private Long expirationTime;

    @Message
    private String consumerTypeAttrDescription;

    private ThreadLocal<DataStore> localDataStore;

    public CacheScope getCacheScope()
    {
        return cacheScope;
    }

    public void setCacheScope(CacheScope cacheScope)
    {
        this.cacheScope = cacheScope;
    }

    public Long getExpirationTime()
    {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime)
    {
        this.expirationTime = expirationTime;
    }

    public Long getWaitTimeout()
    {
        return waitTimeout;
    }

    public void setWaitTimeout(Long waitTimeout)
    {
        this.waitTimeout = waitTimeout;
    }

    @Override
    protected void initFields()
    {
        super.initFields();

        localDataStore = new ThreadLocal<DataStore>();
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttribute attr = new NodeAttributeImpl(CONSUMER_TYPE_ATTR, Boolean.class, false, null);
        attr.setRequired(true);
        MessageComposer desc =
                new MessageComposer(messages)
                .append(messages.createMessageKeyForStringValue(
                    this.getClass().getName(), CONSUMER_TYPE_ATTR));
        attr.setDescriptionContainer(desc);
        consumerAttributes.add(attr);
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes)
        throws Exception
    {
        NodeAttribute activeConsumerAttr = attributes.get(CONSUMER_TYPE_ATTR);
        Boolean activeConsumer = activeConsumerAttr.getRealValue();
        DataState dataState = getDataState(activeConsumer, dataConsumer);
        if (dataState.getDataStore()!=null)
            replayDataToConsumer(dataConsumer, dataState.getDataStore());
        else
        {
            if (activeConsumer && !dataState.isPreparing())
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Gathering data for cache using active consumer (%s)"
                            , dataConsumer.getPath()));
                try
                {
                    localDataStore.set(new MemoryDataStore());
                    getDataSource().getDataImmediate(this, attributes.values());
                    cacheDataStore();
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug("Data cached");
                    replayDataToConsumer(dataConsumer, localDataStore.get());
                }
                finally
                {
                    localDataStore.remove();
                    removePreparingFlag();
                }
            }
            else
            {
                long waitInterval = 0l;
                long waitStartTime = System.currentTimeMillis();
                long _waitTimeout = waitTimeout;
                DataStore dataStore = null;
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug(String.format(
                            "Data consumer (%s) is waiting for data", dataConsumer.getPath()));
                while (dataStore==null && waitInterval<=_waitTimeout)
                {
                    TimeUnit.MILLISECONDS.sleep(100l);
                    dataStore = getDataState(false, dataConsumer).getDataStore();
                    waitInterval=System.currentTimeMillis()-waitStartTime;
                }
                if (dataStore==null)
                {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format(
                                "Error sending cached data to consumer (%s) because " +
                                "of data wait timeout"
                                , dataConsumer.getPath()));
                }
                else
                    replayDataToConsumer(dataConsumer, dataStore);
            }
        }
        return true;
    }

    private synchronized void cacheDataStore()
    {
        temporaryCacheManager.getCache(cacheScope).put(
                DATA_STORE_ATTR, localDataStore.get(), expirationTime);
    }

    private synchronized DataState getDataState(Boolean activeConsumer, DataConsumer consumer)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format(
                    "Data consumer (%s) is %s consumer"
                    , consumer.getPath()
                    , (activeConsumer? "ACTIVE" : "PASSIVE")));
        
        TemporaryCache tempCache = temporaryCacheManager.getCache(cacheScope);
        DataStore store = (DataStore) tempCache.get(DATA_STORE_ATTR);
        if (store!=null)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("Data found in cache for consumer (%s)", consumer.getPath()));
            return new DataState(false, store);
        }
        else
        {
            Cache cache = cacheManager.getCache(cacheScope);
            CacheEntity<Boolean> preparingFlag = cache.get(PREPARING_FLAG_ATTR);
            if (preparingFlag!=null)
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Data preparing");
                return new DataState(true, null);
            }else
            {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    debug("Data not found in cache for consumer");
                return new DataState(false, null);
            }
        }
    }

    private void removePreparingFlag()
    {
        cacheManager.getCache(cacheScope).remove(PREPARING_FLAG_ATTR);
    }

    private void replayDataToConsumer(DataConsumer dataConsumer, DataStore dataStore)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Replaying cached data to consumer (%s)", dataConsumer.getPath()));
        try
        {
            dataStore.open();
            try
            {
                Iterator it = dataStore.getDataIterator();
                while (it.hasNext())
                    dataConsumer.setData(this, it.next());
            }
            finally
            {
                dataStore.close();
            }
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error replaying cached data to consumer (%s)", dataConsumer.getPath()), e);
        }
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        try
        {
            localDataStore.get().addDataPortion(data);
        }
        catch (DataStoreException ex)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error("Data caching error", ex);
        }
    }

    private class DataState
    {
        private final boolean preparing;
        private final DataStore dataStore;

        public DataState(boolean preparing, DataStore dataStore)
        {
            this.preparing = preparing;
            this.dataStore = dataStore;
        }

        public DataStore getDataStore()
        {
            return dataStore;
        }

        public boolean isPreparing()
        {
            return preparing;
        }
    }
}
