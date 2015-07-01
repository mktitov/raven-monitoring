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

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.DataContext;
import org.raven.table.TableImpl;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Mikhail Titov
 */
public class CollectionDecomposerNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private CollectionDecomposerNode decomposer;
    private DataCollector collector;

    @Before
    public void prepare()
    {
        ds = new PushDataSource();
        ds.setName("dataSource");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        decomposer = new CollectionDecomposerNode();
        decomposer.setName("decomposer");
        tree.getRootNode().addAndSaveChildren(decomposer);
        decomposer.setDataSource(ds);
        assertTrue(decomposer.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(decomposer);
        assertTrue(collector.start());
    }

    @Test
    public void nonCollectionDataTest()
    {
        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(eq("hello"), isA(DataContext.class));
        handler.handleData(isNull(), isA(DataContext.class));

        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData("hello");
        ds.pushData(null);

        verify(handler);
    }

    @Test
    public void collectionTest()
    {
        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(eq("hello"), isA(DataContext.class));
        handler.handleData(isNull(), isA(DataContext.class));

        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData(Arrays.asList("hello", null));

        verify(handler);
    }
    
    @Test
    public void iteratorTest()
    {
        DataHandler handler = createMock(DataHandler.class);
        handler.handleData(eq("hello"), isA(DataContext.class));
        handler.handleData(isNull(), isA(DataContext.class));

        replay(handler);

        collector.setDataHandler(handler);
        ds.pushData(Arrays.asList("hello", null).iterator());

        verify(handler);
    }

    @Test
    public void tableTest()
    {
        DataHandler handler = createMock(DataHandler.class);
        Object[] row = new Object[]{"hello"};
        handler.handleData(eq(row), isA(DataContext.class));

        replay(handler);

        collector.setDataHandler(handler);
        TableImpl table = new TableImpl(new String[]{"col1"});
        table.addRow(row);
        ds.pushData(table);

        verify(handler);
    }
}