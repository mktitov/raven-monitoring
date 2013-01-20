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
import org.raven.ds.impl.AttributeValueDataSourceNode;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.Authentication;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.test.PushOnDemandDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ServicesNode;
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
        NetworkResponseServiceNode responseNode = getSRINode();
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
        NetworkResponseServiceNode responseServiceNode = getSRINode();
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName("context");
        responseServiceNode.addAndSaveChildren(context);
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
        assertEquals("test", responseService.getResponse("context", "1.1.1.1", params).getContent());
    }

    @Test
    public void getSubcontextTest() throws NetworkResponseServiceExeption, Exception
    {
        NetworkResponseServiceNode responseServiceNode = getSRINode();
        NetworkResponseContextNode context = createResponseNode(responseServiceNode, "context");
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param", "response");
        assertEquals(
                "subcontextName",
                responseService.getResponse("context/subcontextName", "1.1.1.1", params).getContent());
    }
    
    @Test
    public void contextGroupTest() throws Exception {
        NetworkResponseServiceNode sriNode = getSRINode();
        NetworkResponseGroupNode group = new NetworkResponseGroupNode();
        group.setName("group");
        sriNode.addAndSaveChildren(group);
        assertTrue(group.start());
        
        NetworkResponseContextNode context = createResponseNode(group, "context");
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param", "response");
        assertEquals(
                "subcontextName",
                responseService.getResponse("group/context/subcontextName", "1.1.1.1", params).getContent());
    }
    
    @Test
    public void ifNodeTest() throws Exception {
        NetworkResponseServiceNode sri = getSRINode();
        IfNode if1 = createIfNode(sri, "if1", "requesterIp=='1.1.1.1'");
        createResponseNode(if1, "context1");
        if1 = createIfNode(sri, "if2", "requesterIp=='1.1.1.2'");
        createResponseNode(if1, "context2");
        assertEquals(
                "subcontext1",
                responseService.getResponse("context1/subcontext1", "1.1.1.1", new HashMap<String, Object>())
                    .getContent());
        assertEquals(
                "subcontext2",
                responseService.getResponse("context2/subcontext2", "1.1.1.2", new HashMap<String, Object>())
                    .getContent());
        try {
            responseService.getResponse("context2/subcontext1", "1.1.1.1", new HashMap<String, Object>());
            fail();
        } catch (NetworkResponseServiceExeption e) {}
    }

    @Test
    public void authTest() throws NetworkResponseServiceExeption, Exception
    {
        NetworkResponseServiceNode responseServiceNode = getSRINode();

        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName("context");
        responseServiceNode.addAndSaveChildren(context);
        context.setAllowRequestsFromAnyIp(true);
        context.setDataSource(ds);
        assertTrue(context.start());

        assertNull(responseService.getAuthentication("context", "1.1.1.1"));

        context.setNeedsAuthentication(true);
        context.getNodeAttribute(AbstractNetworkResponseContext.USER_ATTR).setValue("user_name");
        context.getNodeAttribute(AbstractNetworkResponseContext.PASSWORD_ATTR).setValue("pass");

        Authentication auth = context.getAuthentication();
        assertNotNull(auth);
        assertEquals("user_name", auth.getUser());
        assertEquals("pass", auth.getPassword());
    }
    
    @Test
    public void ifNodeWithAuthTest() throws Exception {
        NetworkResponseServiceNode responseServiceNode = getSRINode();

        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        IfNode if1 = createIfNode(responseServiceNode, "if1", "requesterIp=='1.1.1.1'");

        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName("context");
        if1.addAndSaveChildren(context);
        context.setAllowRequestsFromAnyIp(true);
        context.setDataSource(ds);
        assertTrue(context.start());

        assertNull(responseService.getAuthentication("context", "1.1.1.1"));

        context.setNeedsAuthentication(true);
        context.getNodeAttribute(AbstractNetworkResponseContext.USER_ATTR).setValue("user_name");
        context.getNodeAttribute(AbstractNetworkResponseContext.PASSWORD_ATTR).setValue("pass");

        assertNotNull(responseService.getAuthentication("context", "1.1.1.1"));
        try {
            responseService.getAuthentication("context", "1.1.1.2");
            fail();
        } catch (NetworkResponseServiceExeption e) {}
        
        Authentication auth = context.getAuthentication();
        assertNotNull(auth);
        assertEquals("user_name", auth.getUser());
        assertEquals("pass", auth.getPassword());
        
    }
    
    private NetworkResponseContextNode createResponseNode(Node owner, String name) throws Exception {
        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName(name);
        owner.addAndSaveChildren(context);
        context.setAllowRequestsFromAnyIp(true);

        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        context.addAndSaveChildren(ds);
        NodeAttribute expr = ds.getNodeAttribute("value");
        expr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        expr.setValue("params['subcontext']");
        assertTrue(ds.start());

        context.setDataSource(ds);
        assertTrue(context.start());
        return context;
    }
    
    private IfNode createIfNode(Node owner, String name, String expression) throws Exception {
        IfNode if1 = new IfNode();
        if1.setName(name);
        owner.addAndSaveChildren(if1);
        if1.setUsedInTemplate(false);
        NodeAttribute expr = if1.getNodeAttribute(IfNode.EXPRESSION_ATTRIBUTE);
        expr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        expr.setValue(expression);
        assertTrue(if1.start());
        return if1;
    }
    
    private NetworkResponseServiceNode getSRINode() {
        NetworkResponseServiceNode responseServiceNode = (NetworkResponseServiceNode)
                tree.getRootNode()
                .getChildren(SystemNode.NAME)
                .getChildren(ServicesNode.NAME)
                .getChildren(NetworkResponseServiceNode.NAME);
        assertNotNull(responseServiceNode);
        responseServiceNode.setLogLevel(LogLevel.TRACE);
        return responseServiceNode;
    }

}