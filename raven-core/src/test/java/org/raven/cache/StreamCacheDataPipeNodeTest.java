/*
 *  Copyright 2011 Mikhail Titov.
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

import org.raven.auth.UserContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.activation.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestScheduler;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.test.UserContextServiceModule;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class StreamCacheDataPipeNodeTest extends RavenCoreTestCase
{
    private TemporaryFileManagerNode fileManager;
    private StreamCacheDataPipeNode cache;
    private PushDataSource ds;
    private DataCollector collector;
    private InputStream inputStream;

    @Before
    public void prepare()
    {
        inputStream = new ByteArrayInputStream(new byte[]{1,2,3});

        TestScheduler scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        fileManager = new TemporaryFileManagerNode();
        fileManager.setName("file manager");
        tree.getRootNode().addAndSaveChildren(fileManager);
        fileManager.setDirectory("target");
        fileManager.setScheduler(scheduler);
        assertTrue(fileManager.start());

        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        cache = new StreamCacheDataPipeNode();
        cache.setName("cache");
        tree.getRootNode().addAndSaveChildren(cache);
        cache.setDataSource(ds);
        cache.setTemporaryFileManager(fileManager);
        assertTrue(cache.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(cache);
        assertTrue(collector.start());
    }

    @Test
    public void sequencePolicyTest() throws Exception
    {
        String id = ""+cache.getId()+"_";

        ds.pushData(inputStream);
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof DataSource);
        assertNotNull(fileManager.getDataSource(id+"1"));
        ds.pushData(inputStream);
        assertNotNull(fileManager.getDataSource(id+"2"));
    }

    @Test
    public void dataSourceNamePolicyTest() throws Exception
    {
        String id = ""+cache.getId()+"_";
        
        cache.setCacheKeyGenerationPolicy(CacheKeyGenerationPolicy.DATASOURCE_NAME);
        ds.pushData(inputStream);
        assertNotNull(fileManager.getDataSource(id+ds.getPath()));
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof DataSource);
    }

    @Test
    public void dataSourceNameAndUserNamePolicyTest() throws Exception
    {
        String id = ""+cache.getId()+"_";
        
        UserContext userContext = createMock(UserContext.class);
        expect(userContext.getLogin()).andReturn("user");

        replay(userContext);

        UserContextServiceModule.setUserContext(userContext);
        cache.setCacheKeyGenerationPolicy(CacheKeyGenerationPolicy.DATASOURCE_NAME_AND_USER_NAME);
        ds.pushData(inputStream);
        assertNotNull(fileManager.getDataSource(id+ds.getPath()+"_"+"user"));
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof DataSource);

        verify(userContext);
    }
}