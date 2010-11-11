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

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.DataContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.test.DataCollector;
import org.raven.test.DataHandler;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class InitiatePushDataNodeTest extends RavenCoreTestCase
{
    private PushDataSource ds;
    private DataCollector c1, c2;
    private InitiatePushDataNode pipe;

    @Before
    public void prepare() throws Exception
    {
        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        pipe = new InitiatePushDataNode();
        pipe.setName("pipe");
        tree.getRootNode().addAndSaveChildren(pipe);
        pipe.setDataSource(ds);

        c1 = new DataCollector();
        c1.setName("c1");
        tree.getRootNode().addAndSaveChildren(c1);
        c1.setDataSource(pipe);
        assertTrue(c1.start());

        c2 = new DataCollector();
        c2.setName("c2");
        tree.getRootNode().addAndSaveChildren(c2);
        c2.getNodeAttribute("dataSource").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        assertTrue(c2.start());

        pipe.setPushDataTo(c2);
        assertTrue(pipe.start());
    }

    @Test
    public void test()
    {
        final List<String> callOrder = new ArrayList<String>();
        c1.setDataHandler(new Handler("c1", callOrder));
        c2.setDataHandler(new Handler("c2", callOrder));
        ds.pushData("test");

        assertArrayEquals(new Object[]{"c2:test", "c1:test"}, callOrder.toArray());
    }

    @Test
    public void useExpressionForPushDataTo_test()
    {
        pipe.setUseExpressionForPushDataTo(Boolean.TRUE);
        pipe.setExpressionForPushDataTo("data+'1'");

        final List<String> callOrder = new ArrayList<String>();
        c1.setDataHandler(new Handler("c1", callOrder));
        c2.setDataHandler(new Handler("c2", callOrder));
        ds.pushData("test");

        assertArrayEquals(new Object[]{"c2:test1", "c1:test"}, callOrder.toArray());
    }

    @Test
    public void skipDataInExpressionForPushDataTo_test()
    {
        pipe.setUseExpressionForPushDataTo(Boolean.TRUE);
        pipe.setExpressionForPushDataTo("SKIP_DATA");

        final List<String> callOrder = new ArrayList<String>();
        c1.setDataHandler(new Handler("c1", callOrder));
        c2.setDataHandler(new Handler("c2", callOrder));
        ds.pushData("test");

        assertArrayEquals(new Object[]{"c1:test"}, callOrder.toArray());
    }

    @Test
    public void loopDetection_test() throws Exception
    {
        SafeDataPipeNode pipe2 = new SafeDataPipeNode();
        pipe2.setName("pipe2");
        tree.getRootNode().addAndSaveChildren(pipe2);
        pipe2.getNodeAttribute("dataSource").setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        assertTrue(pipe2.start());

        pipe.setPushDataTo(pipe2);

        c2.getNodeAttribute("dataSource").setValueHandlerType(null);
        c2.setDataSource(pipe2);
        
        final List<String> callOrder = new ArrayList<String>();
        c1.setDataHandler(new Handler("c1", callOrder));
        c2.setDataHandler(new Handler("c2", callOrder));
        ds.pushData("test");

        assertArrayEquals(new Object[]{"c2:test", "c1:test"}, callOrder.toArray());
    }

    private class Handler implements DataHandler{
        private final String prefix;
        private final List<String> callOrder;

        public Handler(String prefix, List<String> callOrder) {
            this.prefix = prefix;
            this.callOrder = callOrder;
        }

        public void handleData(Object data, DataContext context) {
            callOrder.add(prefix+":"+data);
        }
    }
}