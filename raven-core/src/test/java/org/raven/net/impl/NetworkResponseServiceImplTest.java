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
import javax.script.Bindings;
import org.junit.Before;
import org.junit.Test;
import org.raven.ds.impl.AttributeValueDataSourceNode;
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ServicesNode;
import org.raven.tree.impl.SystemNode;
import static org.easymock.EasyMock.*;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.raven.BindingNames;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.LoginException;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.auth.impl.AccessControl;
import org.raven.auth.impl.AccessRight;
import org.raven.auth.impl.LoginServiceWrapper;
import org.raven.net.AccessDeniedException;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseServiceUnavailableException;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.prj.impl.ProjectNode;
import org.raven.test.PushOnDemandDataSource;
import org.raven.tree.impl.LoggerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class NetworkResponseServiceImplTest extends RavenCoreTestCase {
    private final static Logger logger = LoggerFactory.getLogger(NetworkResponseServiceImplTest.class);
    private final static LoggerHelper loggerHelper = new LoggerHelper(LogLevel.TRACE, "SRI", "[SRI] ", logger);
    
    private NetworkResponseService responseService;
    private NetworkResponseServiceNode responseServiceNode;
    private IMocksControl mocks;
    private Map<String, Object> params;
    private LoginServiceWrapper loginService;
    private ProjectNode project;

    @Before
    public void prepare() {
        responseService = registry.getService(NetworkResponseService.class);
        assertNotNull(responseService);
        responseServiceNode = getSRINode();
        project = new ProjectNode();
        project.setName("project");
        tree.getProjectsNode().addAndSaveChildren(project);
        assertTrue(project.start());
        mocks = createControl();
        params = new HashMap<String, Object>();
        
        loginService = createLoginService("main login service");
    }

    @Test(expected=NetworkResponseServiceUnavailableException.class)
    public void serviceUnavailableTest() throws NetworkResponseServiceExeption {
        responseServiceNode.stop();
        Request request = trainRequest("context");
        mocks.replay();
        responseService.getResponseContext(request);
        mocks.verify();
    }
    
    @Test(expected=ContextUnavailableException.class)
    public void contextUnavailableTest() throws NetworkResponseServiceExeption {
        Request request = trainRequest("context");
        mocks.replay();
        responseService.getResponseContext(request);
        mocks.verify();
    }
    
    //on stopped project
    @Test(expected=ContextUnavailableException.class)
    public void projectsContextUnavailableTest() throws NetworkResponseServiceExeption {
        project.stop();
        createSimpleResponseBuilder(project.getWebInterface(), "context");
        Request request = trainRequest("project/context", true);
        mocks.replay();
        responseService.getResponseContext(request);
        mocks.verify();
    }
    
    //on stopped web interface
    @Test(expected=ContextUnavailableException.class)
    public void projectsContextUnavailableTest2() throws NetworkResponseServiceExeption {
        project.getWebInterface().stop();
        createSimpleResponseBuilder(project.getWebInterface(), "context");
        Request request = trainRequest("project/context", true);
        mocks.replay();
        responseService.getResponseContext(request);
        mocks.verify();
    }
    
    @Test()
    public void projectsGetContextSuccessTest() throws NetworkResponseServiceExeption {
        SimpleResponseBuilder builder = createSimpleResponseBuilder(project.getWebInterface(), "context");
        builder.setLoginService(loginService);
        Request request = trainRequest("project/context", true);
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        ResponseContext response = responseService.getResponseContext(request);
        assertSame(builder, response.getResponseBuilder());
        assertSame(loginService, response.getLoginService());
        mocks.verify();
    }
    
    @Test(expected = AccessDeniedException.class)
    public void projectsAccessDeniedTest() throws NetworkResponseServiceExeption {
        SimpleResponseBuilder builder = createSimpleResponseBuilder(project.getWebInterface(), "context");
        Request request = trainRequest("project/context", true);
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        ResponseContext response = responseService.getResponseContext(request);
        assertSame(builder, response.getResponseBuilder());
        assertSame(loginService, response.getLoginService());
        mocks.verify();
    }
    
    @Test(expected=ContextUnavailableException.class)
    public void contextUnavailable_lastElementIsGroupTest() throws NetworkResponseServiceExeption {
        Request request = trainRequest("context", "GET");
        mocks.replay();
        createGroup(responseServiceNode, "context");
        responseService.getResponseContext(request);
        mocks.verify();
    }
    
    //Testing context without login service
    @Test(expected = AccessDeniedException.class)
    public void authenticationFailedTest() throws NetworkResponseServiceExeption {
        Request request = trainRequest("context");
        mocks.replay();
        SimpleResponseBuilder builder = createSimpleResponseBuilder(responseServiceNode, "context");
        responseService.getResponseContext(request);
        mocks.verify();
    }
    
    //Success response context get test
    @Test
    public void getResponseContext() throws NetworkResponseServiceExeption {
        Request request = trainRequest("context");
        mocks.replay();
        SimpleResponseBuilder builder = createSimpleResponseBuilder(responseServiceNode, "context");
        builder.setLoginService(loginService);
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        mocks.verify();
    }
    
    @Test
    public void loginServiceInheritanceTest() throws NetworkResponseServiceExeption {
        Request request = trainRequest("group/context");
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "group");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "context");
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        assertSame(builder, responseContext.getResponseBuilder());
        mocks.verify();
    }
    
    @Test
    public void getResponseBuilderByHttpMethod() throws NetworkResponseServiceExeption {
        Request request = trainRequest("group", "GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "group");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "?GET");
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        assertSame(builder, responseContext.getResponseBuilder());
        mocks.verify();
    }
    
    @Test
    public void getResponseBuilderByHttpMethod_UnknownPath() throws NetworkResponseServiceExeption {
        Request request = trainRequest("group/test", "GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "group");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "?GET");
        builder.setCanHandleUnknownPath(Boolean.TRUE);
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        assertSame(builder, responseContext.getResponseBuilder());
        assertEquals("test", responseContext.getSubcontextPath());
        mocks.verify();
    }
    
    @Test(expected = ContextUnavailableException.class)
    public void getResponseBuilderByHttpMethod_UnknownPath2() throws NetworkResponseServiceExeption {
        Request request = trainRequest("group/test", "GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "group");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "?GET");
        
        ResponseContext responseContext = responseService.getResponseContext(request);
    }
    
    @Test
    public void namedParametersTest() throws NetworkResponseServiceExeption {
        Request request = trainRequest("test/1");
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "{param1}");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "{param2}");
        builder.setNamedParameterType(Integer.class);
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        assertSame(builder, responseContext.getResponseBuilder());
        assertEquals("test", request.getParams().get("param1"));
        assertEquals(new Integer(1), request.getParams().get("param2"));
        mocks.verify();
    }
    
    @Test
    public void namedParameterPatternTest() throws NetworkResponseServiceExeption {
        Request request = trainRequest("test/1.js");
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "{param1}");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "{param2}");
        builder.setNamedParameterType(Integer.class);
        builder.setNamedParameterPattern("(.*)\\.js");
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        assertSame(builder, responseContext.getResponseBuilder());
        assertEquals("test", request.getParams().get("param1"));
        assertEquals(new Integer(1), request.getParams().get("param2"));
        mocks.verify();
    }
    
    @Test
    public void namedParameterPattern2Test() throws NetworkResponseServiceExeption {
        Request request = trainRequest("test/1");
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "{param1}");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "{param2}");
        builder.setNamedParameterType(Integer.class);
        builder.setNamedParameterPattern("1");
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        assertSame(loginService, responseContext.getLoginService());
        assertSame(request, responseContext.getRequest());
        assertSame(builder, responseContext.getResponseBuilder());
        assertEquals("test", request.getParams().get("param1"));
        assertEquals(new Integer(1), request.getParams().get("param2"));
        mocks.verify();
    }
    
    @Test(expected=ContextUnavailableException.class)
    public void namedParameterPattern3Test() throws NetworkResponseServiceExeption {
        Request request = trainRequest("test/1");
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        
        NetworkResponseGroupNode group = createGroup(responseServiceNode, "{param1}");
        group.setLoginService(loginService);
        SimpleResponseBuilder builder = createSimpleResponseBuilder(group, "{param2}");
        builder.setNamedParameterType(Integer.class);
        builder.setNamedParameterPattern("2");
        
        ResponseContext responseContext = responseService.getResponseContext(request);
//        assertNotNull(responseContext);
//        assertSame(loginService, responseContext.getLoginService());
//        assertSame(request, responseContext.getRequest());
//        assertSame(builder, responseContext.getResponseBuilder());
//        assertEquals("test", request.getParams().get("param1"));
//        assertEquals(new Integer(1), request.getParams().get("param2"));
        mocks.verify();
    }
    
    //ResponseBuilder.getAccessRight returns NULL
    @Test
    public void accessNotGrantedTest() throws Exception {
        assertFalse(testAccessGranted(null, AccessControl.READ));
    }
    
    //granted access lower then needed
    @Test
    public void accessNotGrantedTest2() throws Exception {
        assertFalse(testAccessGranted(AccessRight.READ, AccessControl.WRITE));
    }
    
    @Test
    public void accessGrantedTest() throws Exception {
        assertTrue(testAccessGranted(AccessRight.READ, AccessControl.READ));
    }
    
    @Test
    public void accessGrantedTest2() throws Exception {
        assertTrue(testAccessGranted(AccessRight.WRITE, AccessControl.WRITE|AccessControl.READ));
    }
    
    @Test
    public void getResponseTest() throws Exception {
//        Request request = trainRequest("context");
        ResponseBuilderWrapperNode responseBuilder = createResponseBuilderWrapper(responseServiceNode, "context");
        ResponseBuilderWrapperNode.ResponseBuilderWrapper wrapper = mocks.createMock(
                ResponseBuilderWrapperNode.ResponseBuilderWrapper.class);
        Response response = mocks.createMock(Response.class);
        UserContext user = mocks.createMock(UserContext.class);
        LoginService loginService = mocks.createMock(LoginService.class);
        Request request = mocks.createMock(Request.class);
        Map<String, Object> params = mocks.createMock(Map.class);
        
        responseServiceNode.setLogLevel(LogLevel.ERROR);
        ResponseContextImpl responseContext = new ResponseContextImpl(
                request, "context", "sub", 1, loginService, responseBuilder, responseServiceNode);
        
        expect(request.getParams()).andReturn(params).atLeastOnce();
        expect(request.getRootPath()).andReturn("/raven").anyTimes();
        expect(request.getServicePath()).andReturn(Request.SRI_SERVICE).anyTimes();
        expect(request.getContextPath()).andReturn(null).anyTimes();
        expect(params.get(BindingNames.APP_NODE)).andReturn(null).anyTimes();
        expect(params.get(BindingNames.APP_PATH)).andReturn(null).anyTimes();
        
        expect(params.put(NetworkResponseServiceNode.SUBCONTEXT_PARAM, "sub")).andReturn(null);
        expect(user.getLogin()).andReturn("Test user").anyTimes();
        expect(wrapper.buildResponse(same(user), same(responseContext), checkBindings(user, request, responseContext))).andReturn(response);
        mocks.replay();
        responseBuilder.setWrappedResponseBuilder(wrapper);
        Response result = responseContext.getResponse(user);
        assertSame(response, result);
        mocks.verify();
    }
    
    public static Bindings checkBindings(final UserContext user, final Request request, final ResponseContext responseContext) {
        reportMatcher(new IArgumentMatcher() {
            public boolean matches(Object obj) {
                assertNotNull(obj);
                Bindings bindings = (Bindings) obj;
                assertSame(user, bindings.get(BindingNames.USER_CONTEXT));
                assertSame(request, bindings.get(BindingNames.REQUEST_BINDING));
                assertSame(responseContext, bindings.get(BindingNames.RESPONSE_BINDING));
                return true;
            }
            public void appendTo(StringBuffer buffer) {
            }
        });
        return null;
    }
    
    @Test
    public void getOldResponseTest() throws Exception
    {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
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
        
        Request request = trainRequest("context", params);
        mocks.replay();
        Response resp = getResponse(request);
        assertEquals("test", resp.getContent());
        
//        assertEquals("test", responseService.getResponseContext("context", "1.1.1.1", params).getContent());
        mocks.verify();
    }

    @Test
    public void getOldSubcontextTest() throws Exception {
        NetworkResponseContextNode context = createResponseNode(responseServiceNode, "context");
        Request request = trainRequest("context/subcontextName");
        mocks.replay();
        Response resp = getResponse(request);
        
        assertEquals("subcontextName", resp.getContent());
        
        mocks.verify();
    }
    
    @Test
    public void oldContextGroupTest() throws Exception {
        NetworkResponseServiceNode sriNode = getSRINode();
        NetworkResponseGroupNode group = new NetworkResponseGroupNode();
        group.setName("group");
        sriNode.addAndSaveChildren(group);
        assertTrue(group.start());
        NetworkResponseContextNode context = createResponseNode(group, "context");
        Request request = trainRequest("group/context/subcontextName");
        expect(request.getMethod()).andReturn("GET");
        mocks.replay();
        
        Response resp = getResponse(request);
        assertEquals("subcontextName", resp.getContent());
        
        mocks.verify();
    }
    
    @Test
    public void oldIfNodeTest() throws Exception {
        NetworkResponseServiceNode sri = getSRINode();
        IfNode if1 = createIfNode(sri, "if1", "requesterIp=='1.1.1.1'");
        createResponseNode(if1, "context1");
        if1 = createIfNode(sri, "if2", "requesterIp=='1.1.1.2'");
        createResponseNode(if1, "context2");
        
        Request request1 = trainRequest("context1/subcontext1");
        Request request2 = trainRequestWithRemoteIp("context2/subcontext2", "1.1.1.2");
        Request request3 = trainRequest("context2/subcontext1");
        mocks.replay();
        assertEquals("subcontext1", getResponse(request1, "1.1.1.1").getContent());
        assertEquals("subcontext2", getResponse(request2, "1.1.1.2").getContent());
        try {
             getResponse(request3, "1.1.1.1").getContent();
        } catch (NetworkResponseServiceExeption e) {}
        mocks.verify();
    }

    @Test
    public void authTest() throws NetworkResponseServiceExeption, Exception {
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
        context.setNeedsAuthentication(true);
        context.getAttr(AbstractNetworkResponseContext.USER_ATTR).setValue("user_name");
        context.getAttr(AbstractNetworkResponseContext.PASSWORD_ATTR).setValue("pass");
        
        Request request = trainRequest("context");
        mocks.replay();
        
        ResponseContext responseContext = responseService.getResponseContext(request);
        LoginService loginService = responseContext.getLoginService();
        assertNotNull(loginService);
        checkForInvalid(loginService, "invalid_user", "pass", "1.1.1.1");
        checkForInvalid(loginService, "user_name", "invalid_pass", "1.1.1.1");
        checkForInvalid(loginService, null, "pass", "1.1.1.1");
        checkForInvalid(loginService, "user_name", null, "1.1.1.1");
        UserContext user = loginService.login("user_name", "pass", null);
        assertTrue(responseContext.isAccessGranted(user));
        mocks.verify();
    }
    
    @Test
    public void ifNodeWithAuthTest() throws Exception {
        PushOnDemandDataSource ds = new PushOnDemandDataSource();
        ds.setName("ds");
        testsNode.addAndSaveChildren(ds);
        assertTrue(ds.start());
        
        IfNode if1 = createIfNode(responseServiceNode, "if1", "requesterIp=='1.1.1.1'");

        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName("context");
        if1.addAndSaveChildren(context);
        context.setAllowRequestsFromAnyIp(true);
        context.setDataSource(ds);
        assertTrue(context.start());
        
        Request request1 = trainRequest("context");
        Request request2 = trainRequest("context");
        Request request3 = trainRequestWithRemoteIp("context", "1.1.1.2");
        mocks.replay();
        ResponseContext responseContext = responseService.getResponseContext(request1);
        LoginService loginService = responseContext.getLoginService();
        UserContext user = loginService.login("any_user", "any_pass", "any_ip");
        assertTrue(responseContext.isAccessGranted(user));
        
        context.setNeedsAuthentication(true);
        context.getAttr(AbstractNetworkResponseContext.USER_ATTR).setValue("user_name");
        context.getAttr(AbstractNetworkResponseContext.PASSWORD_ATTR).setValue("pass");
        
        responseContext = responseService.getResponseContext(request2);
        loginService = responseContext.getLoginService();
        user = loginService.login("user_name", "pass", "1.1.1.2");
        assertTrue(responseContext.isAccessGranted(user));
        
        try {
            responseContext = responseService.getResponseContext(request3);
            fail();
        } catch (NetworkResponseServiceExeption e) {}
        
        mocks.verify();
    }
    
    private boolean testAccessGranted(AccessRight builderRights, int rights) throws Exception {
        SimpleResponseBuilder builder = createSimpleResponseBuilder(responseServiceNode, "context");
        builder.setMinimalAccessRight(builderRights);
        Request request = trainRequest("context");
        UserContext user = mocks.createMock(UserContext.class);
        if (builderRights!=null) 
            expect(user.getAccessForNode(builder)).andReturn(rights);
        
        mocks.replay();
        
        builder.setLoginService(loginService);
        ResponseContext responseContext = responseService.getResponseContext(request);
        assertNotNull(responseContext);
        boolean res = responseContext.isAccessGranted(user);
        
        mocks.verify();
        return res;
    }
    
    private Request trainRequest(String contextPath) {
        return trainRequest(contextPath, false);
    }
    
    private Request trainRequest(String contextPath, boolean projectsService) {
        Request request = mocks.createMock(Request.class);
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getRemoteAddr()).andReturn("1.1.1.1").anyTimes();
        expect(request.getParams()).andReturn(params).anyTimes();
        expect(request.getServicePath()).andReturn(projectsService? Request.PROJECTS_SERVICE : Request.SRI_SERVICE).anyTimes();
        expect(request.getRootPath()).andReturn("/raven").anyTimes();
        return request;
    }
    
    private Request trainRequestWithRemoteIp(String contextPath, String remoteIp) {
        Request request = mocks.createMock(Request.class);
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getRemoteAddr()).andReturn(remoteIp).anyTimes();
        expect(request.getParams()).andReturn(params).anyTimes();
        expect(request.getServicePath()).andReturn(Request.SRI_SERVICE).anyTimes();
        expect(request.getRootPath()).andReturn("/raven").anyTimes();
        return request;
    }
    
    private Request trainRequest(String contextPath, Map<String, Object> params) {
        Request request = mocks.createMock(Request.class);
        expect(request.getContextPath()).andReturn(contextPath).anyTimes();
        expect(request.getRemoteAddr()).andReturn("1.1.1.1").anyTimes();
        expect(request.getParams()).andReturn(params).atLeastOnce();
        expect(request.getServicePath()).andReturn(Request.SRI_SERVICE).anyTimes();
        expect(request.getRootPath()).andReturn("/raven").anyTimes();
        return request;
    }
    
    private Request trainRequest(String contextPath, String method) {
        Request request = trainRequest(contextPath);
        expect(request.getMethod()).andReturn(method);
        return request;
    }
    
    private UserContext trainUserContext() {
        UserContext user = mocks.createMock(UserContext.class);
        return user;
    }
    
    private NetworkResponseGroupNode createGroup(Node owner, String name) {
        NetworkResponseGroupNode group = new NetworkResponseGroupNode();
        group.setName(name);
        owner.addAndSaveChildren(group);
        assertTrue(group.start());
        return group;
    }
    
    private NetworkResponseContextNode createResponseNode(Node owner, String name) throws Exception {
        NetworkResponseContextNode context = new NetworkResponseContextNode();
        context.setName(name);
        owner.addAndSaveChildren(context);
        context.setAllowRequestsFromAnyIp(true);

        AttributeValueDataSourceNode ds = new AttributeValueDataSourceNode();
        ds.setName("ds");
        context.addAndSaveChildren(ds);
        NodeAttribute expr = ds.getAttr("value");
        expr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        expr.setValue("params['subcontext']");
        assertTrue(ds.start());

        context.setDataSource(ds);
        assertTrue(context.start());
        return context;
    }
    
    private SimpleResponseBuilder createSimpleResponseBuilder(Node owner, String name) {
        SimpleResponseBuilder builder = new SimpleResponseBuilder();
        builder.setName(name);
        owner.addAndSaveChildren(builder);
        builder.setResponseContentType("text/plain");
        assertTrue(builder.start());
        return builder;
    }
    
    private ResponseBuilderWrapperNode createResponseBuilderWrapper(Node owner, String name) {
        ResponseBuilderWrapperNode wrapper = new ResponseBuilderWrapperNode();
        wrapper.setName(name);
        owner.addAndSaveChildren(wrapper);
        assertTrue(wrapper.start());
        return wrapper;
    }
    
    private IfNode createIfNode(Node owner, String name, String expression) throws Exception {
        IfNode if1 = new IfNode();
        if1.setName(name);
        owner.addAndSaveChildren(if1);
        if1.setUsedInTemplate(false);
        NodeAttribute expr = if1.getAttr(IfNode.EXPRESSION_ATTRIBUTE);
        expr.setValueHandlerType(ScriptAttributeValueHandlerFactory.TYPE);
        expr.setValue(expression);
        assertTrue(if1.start());
        return if1;
    }
    
    private NetworkResponseServiceNode getSRINode() {
        NetworkResponseServiceNode responseServiceNode = (NetworkResponseServiceNode)
                tree.getRootNode()
                .getNode(SystemNode.NAME)
                .getNode(ServicesNode.NAME)
                .getNode(NetworkResponseServiceNode.NAME);
        assertNotNull(responseServiceNode);
        responseServiceNode.setLogLevel(LogLevel.TRACE);
        return responseServiceNode;
    }

    private LoginServiceWrapper createLoginService(String name) {
        LoginServiceWrapper loginService = new LoginServiceWrapper();
        loginService.setName(name);
        testsNode.addAndSaveChildren(loginService);
        assertTrue(loginService.start());
        return loginService;
    }

    private Response getResponse(Request request) throws Exception {
        return getResponse(request, "1.1.1.1");
    }

    private Response getResponse(Request request, String remoteIp) throws Exception {
        ResponseContext responseContext = responseService.getResponseContext(request);
        UserContext user = responseContext.getLoginService().login("test", "test_pwd", remoteIp);
        assertTrue(responseContext.isAccessGranted(user));
        Response resp = responseContext.getResponse(user);
        return resp;
    }
    
    private void checkForInvalid(LoginService loginService, String user, String pass, String remoteIp) 
            throws LoginException 
    {
        try {
            loginService.login(user, pass, remoteIp);
            fail();
        } catch (AuthenticationFailedException ex) {}
    } 
}