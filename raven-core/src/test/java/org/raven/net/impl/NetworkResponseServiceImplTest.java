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

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.raven.PushOnDemandDataSource;
import org.raven.RavenCoreTestCase;
import org.raven.log.LogLevel;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.tree.impl.SystemNode;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServiceImplTest extends RavenCoreTestCase
{
    NetworkResponseService responseService;

    @Before
    public void prepare()
    {
        responseService = registry.getService(NetworkResponseService.class);
        assertNotNull(responseService);
    }

//    @Test(expected=NetworkResponseServiceUnavailableException.class)
//    public void serviceUnavailableTest() throws NetworkResponseServiceExeption
//    {
//        responseService.getResponse("context", "1.1.1.1", null);
//    }

    @Test(expected=NetworkResponseServiceUnavailableException.class)
    public void serviceUnavailableTest2() throws NetworkResponseServiceExeption
    {
        NetworkResponseServiceNode responseNode = (NetworkResponseServiceNode) 
                tree.getRootNode().getChildren(SystemNode.NAME)
                .getChildren(NetworkResponseServiceNode.NAME);
        assertNotNull(responseNode);
        responseNode.stop();
        NetworkResponseServiceNode responseServiceNode = new NetworkResponseServiceNode();
        responseServiceNode.setName("responseService");
        tree.getRootNode().addAndSaveChildren(responseServiceNode);
        
        responseService.getResponse("context", "1.1.1.1", null);
    }

    @Test(expected=ContextUnavailableException.class)
    public void contextUnavailableTest() throws NetworkResponseServiceExeption
    {
        NetworkResponseServiceNode responseServiceNode = new NetworkResponseServiceNode();
        responseServiceNode.setName("responseService");
        tree.getRootNode().addAndSaveChildren(responseServiceNode);
        assertTrue(responseServiceNode.start());
        
        responseService.getResponse("context", "1.1.1.1", null);
    }

    @Test
    public void getResponseTest() throws NetworkResponseServiceExeption
    {
        NetworkResponseServiceNode responseServiceNode = (NetworkResponseServiceNode)
                tree.getRootNode().getChildren(SystemNode.NAME)
                .getChildren(NetworkResponseServiceNode.NAME);
        assertNotNull(responseServiceNode);
        responseServiceNode.setLogLevel(LogLevel.TRACE);

        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName("context");
        responseServiceNode.addAndSaveChildren(context);
        context.setUseExpression(true);
        context.setExpression("params['param']+' '+data");
        context.setAllowRequestsFromAnyIp(true);
        context.setDataSource(ds);
        assertTrue(context.start());

        ParameterNode parameter = new ParameterNode();
        parameter.setName("param");
        context.getParametersNode().addAndSaveChildren(parameter);
        parameter.setParameterType(String.class);
        parameter.setRequired(true);
        assertTrue(parameter.start());

        ds.addDataPortion("test");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param", "response");
        assertEquals("response test", responseService.getResponse("context", "1.1.1.1", params));
    }
}