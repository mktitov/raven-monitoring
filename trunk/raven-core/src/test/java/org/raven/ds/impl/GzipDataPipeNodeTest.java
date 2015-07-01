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

package org.raven.ds.impl;

import java.util.zip.GZIPInputStream;
import javax.activation.DataSource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.raven.TestScheduler;
import org.raven.cache.TemporaryFileManagerNode;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class GzipDataPipeNodeTest extends RavenCoreTestCase {
    private TestScheduler scheduler;
    private TemporaryFileManagerNode manager;
    private PushDataSource ds;
    private GzipDataPipeNode gzipPipe;
    private DataCollector collector;    
    
    @Before
    public void prepare() {
        scheduler = new TestScheduler();
        scheduler.setName("scheduler");
        testsNode.addAndSaveChildren(scheduler);
        assertTrue(scheduler.start());

        manager = new TemporaryFileManagerNode();
        manager.setName("manager");
        testsNode.addAndSaveChildren(manager);
        manager.setDirectory("target/");
        manager.setScheduler(scheduler);
        assertTrue(manager.start());        
        
        ds = new PushDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        gzipPipe = new GzipDataPipeNode();
        gzipPipe.setName("gzip");
        testsNode.addAndSaveChildren(gzipPipe);
        gzipPipe.setDataSource(ds);
        gzipPipe.setTemporaryFileManager(manager);
        assertTrue(gzipPipe.start());
        
        collector = new DataCollector();
        collector.setName("collector");
        testsNode.addAndSaveChildren(collector);
        collector.setDataSource(gzipPipe);
        assertTrue(collector.start());
    }
    
    @Test
    public void stringDataTest() throws Exception {
        String testString = "тест";
        ds.pushData(testString);
        assertEquals(1, collector.getDataListSize());
        assertTrue(collector.getDataList().get(0) instanceof DataSource);
        
        DataSource dataSource = (DataSource) collector.getDataList().get(0);
        GZIPInputStream unzipStream = new GZIPInputStream(dataSource.getInputStream());
        String str = IOUtils.toString(unzipStream, gzipPipe.getCharsetForStringData());
        assertEquals(testString, str);
    }
}
