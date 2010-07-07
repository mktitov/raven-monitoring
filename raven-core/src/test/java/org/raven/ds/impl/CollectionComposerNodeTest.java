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

package org.raven.ds.impl;

import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class CollectionComposerNodeTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        CollectionComposerNode composer = new CollectionComposerNode();
        composer.setName("composer");
        tree.getRootNode().addAndSaveChildren(composer);
        composer.setDataSource(ds);
        assertTrue(composer.start());

        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(composer);
        assertTrue(collector.start());

        ds.pushData(1);
        ds.pushData(null);

        List dataList = collector.getDataList();
        assertEquals(1, dataList.size());
        assertTrue(dataList.get(0) instanceof Collection);
        Collection collection = (Collection) dataList.get(0);
        assertEquals(1, collection.size());
        assertEquals(1, collection.iterator().next());
    }
}