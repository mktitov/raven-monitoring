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

package org.raven.ds.impl;

import java.io.ByteArrayInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.TestUserContext;
import org.raven.auth.UserContext;
import org.raven.ds.impl.FileStreamSourceNode.ContextCountingStream;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;
import org.raven.test.UserContextServiceModule;

/**
 *
 * @author Mikhail Titov
 */
public class FileStreamSourceNodeTest extends RavenCoreTestCase
{
    private DataCollector collector;
    private FileStreamSourceNode source;
    private UserContext context;

    @Before
    public void prepare()
    {
        source = new FileStreamSourceNode();
        source.setName("stream source");
        tree.getRootNode().addAndSaveChildren(source);
        assertTrue(source.start());

        collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(source);
        assertTrue(collector.start());

        context = new TestUserContext();
        UserContextServiceModule.setUserContext(context);
    }

    @Test
    public void test() throws Exception
    {
        assertNull(source.getViewableObjects(null));

        byte[] arr = {1,2,3,4,5};
        ByteArrayInputStream in = new ByteArrayInputStream(arr);
        source.getFile().setStream(in);

        assertNotNull(source.getViewableObjects(null));

        assertEquals(1, collector.getDataListSize());
        Object data = collector.getDataList().get(0);
        assertTrue(data instanceof FileStreamSourceNode.ContextCountingStream);
        FileStreamSourceNode.ContextCountingStream stream = (ContextCountingStream) data;
        assertTrue(stream.isTransmitting());
        assertEquals(0, stream.getBytesRead());
        Object param = context.getParams().get(source.getKey());
        assertNotNull(param);
        assertSame(param, stream);

        byte[] res = IOUtils.toByteArray(stream);
        assertArrayEquals(arr, res);
        assertFalse(stream.isTransmitting());
        //10??? must be a 5
        assertEquals(10, stream.getBytesRead());
    }
}