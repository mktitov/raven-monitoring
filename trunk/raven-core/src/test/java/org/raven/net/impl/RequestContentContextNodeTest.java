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

package org.raven.net.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.DataCollector;
import org.raven.test.RavenCoreTestCase;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Response;

/**
 *
 * @author Mikhail Titov
 */
public class RequestContentContextNodeTest extends RavenCoreTestCase
{
    RequestContentContextNode context;

    @Before
    public void prepare()
    {
        context = new RequestContentContextNode();
        context.setName("context");
        tree.getRootNode().addAndSaveChildren(context);
        context.setAllowRequestsFromAnyIp(true);
        assertTrue(context.start());
    }

    @Test
    public void requestContentParameterTest()
    {
        ParameterNode contentParam = (ParameterNode) 
                context.getChildren(ParametersNode.NAME)
                .getChildren(NetworkResponseService.REQUEST_CONTENT_PARAMETER);
        assertNotNull(contentParam);
        assertEquals(InputStream.class, contentParam.getParameterType());
        assertTrue(contentParam.getRequired());
        assertNull(contentParam.getPattern());
    }

    @Test
    public void dataSourceTest() throws NetworkResponseServiceExeption
    {
        DataCollector collector = new DataCollector();
        collector.setName("collector");
        tree.getRootNode().addAndSaveChildren(collector);
        collector.setDataSource(context);
        assertTrue(collector.start());

        Map<String, Object> params = new HashMap<String, Object>();
        InputStream data = new ByteArrayInputStream(new byte[]{1,2,3});
        params.put(NetworkResponseService.REQUEST_CONTENT_PARAMETER, data);

        Object res = context.getResponse("127.0.0.1", params);
        assertTrue(res instanceof Response);
        assertNull(((Response)res).getContent());
        assertEquals(1, collector.getDataList().size());
        assertSame(data, collector.getDataList().get(0));
    }
}