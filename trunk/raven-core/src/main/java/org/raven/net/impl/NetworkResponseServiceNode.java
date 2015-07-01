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
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Bindings;
import org.apache.commons.lang.StringUtils;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.LoginService;
import org.raven.cache.TemporaryFileManager;
import org.raven.cache.TemporaryFileManagerValueHandlerFactory;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.IfNode;
import org.raven.log.LogLevel;
import org.raven.net.AccessDeniedException;
import org.raven.net.Authentication;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseContext;
import org.raven.net.NetworkResponseNode;
import org.raven.net.NetworkResponseService;
import org.raven.net.NetworkResponseServiceExeption;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseBuilder;
import org.raven.net.ResponseContext;
import org.raven.net.ResponseServiceNode;
import org.raven.prj.Project;
import org.raven.prj.impl.WebInterfaceNode;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, childNodes={IfNode.class})
public class NetworkResponseServiceNode extends BaseNode implements NetworkResponseNode, ResponseServiceNode
{
    public final static String NAME = "NetworkResponseService";
    public static final String SUBCONTEXT_PARAM = "subcontext";
    public static final String REQUESTER_IP_BINDING = "requesterIp";

    @Service
    private static NetworkResponseService networkResponseService;
    public static final String REQUEST_PARAMS_BINDING = "requestParams";
    public static final String CONTEXT_BINDING = "requestContext";
    public static final String LOGIN_SERVICE_ATTR = "loginService";
    public static final String HTTP_METHOD_BINDING = "httpMethod";
    public static final String REQUEST_BINDING = "request";
    public static final String NAMED_PARAMETER_TYPE_ATTR = "namedParameterType";
    public static final String NAMED_PARAMETER_PATTERN_ATTR = "namedParameterPattern";
    public static final String NAMED_PARAMETER_VALIDATOR_ATTR = "namedParameterValidator";
    public static final String NAMED_PARAMETER_VALIDATORS_ATTR = "_namedParameterValidators";
    
    private final static NetRespAnonymousLoginService netRespAnonLoginService = new NetRespAnonymousLoginService();
    
    @Parameter(valueHandlerType = TemporaryFileManagerValueHandlerFactory.TYPE)
    private TemporaryFileManager temporaryFileManager;
    
    @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;

    @Parameter(readOnly=true)
    private AtomicLong requestsCount;

    @Parameter(readOnly=true)
    private AtomicLong requestsWithErrors;
    
    
    private BindingSupportImpl bindingSupport;

    public NetworkResponseServiceNode() {
        super(NAME);
    }
    
    public AtomicLong getRequestsCount()
    {
        return requestsCount;
    }

    public AtomicLong getRequestsWithErrors()
    {
        return requestsWithErrors;
    }

    public BindingSupportImpl getBindingSupport() {
        return bindingSupport;
    }

    @Override
    protected void initFields() {
        super.initFields();
        requestsCount = new AtomicLong(0);
        requestsWithErrors = new AtomicLong(0);
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        networkResponseService.setNetworkResponseServiceNode(this);
        requestsCount.set(0);
        requestsWithErrors.set(0);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        networkResponseService.setNetworkResponseServiceNode(null);
    }

    public TemporaryFileManager getTemporaryFileManager() {
        return temporaryFileManager;
    }

    public void setTemporaryFileManager(TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
    
    public void incRequestsCountWithErrors() {
        requestsWithErrors.incrementAndGet();
    }
    
    public long getNextRequestId() {
        return requestsCount.incrementAndGet();
    }

    public ResponseContext getResponseContext(Request request) throws NetworkResponseServiceExeption {
        BindingSupport bindings = null;
        try {
            PathInfo pathInfo = new PathInfo();
            ResponseServiceNode serviceNode = null;
            int pathIndex = 0;
            String[] pathElems = splitContextToPathElements(request.getContextPath());
            if (Request.SRI_SERVICE.equals(request.getServicePath())) {
                bindings = bindingSupport;
                serviceNode = this;
            } else {
                Project project = tree.getProjectsNode().getProject(pathElems[0]);
                if (project==null || !project.isStarted())
                    throw new ContextUnavailableException(request, pathElems[0]);
                WebInterfaceNode webi = project.getWebInterface();
                if (webi==null || !webi.isStarted())
                    throw new ContextUnavailableException(request, pathElems[0]);
                bindings = webi.getBindingSupport();
                serviceNode = webi;
                pathIndex = 1;
                PathClosure pathCl = new PathClosure(this, request.getRootPath(), pathResolver, this);
                request.getParams().put(BindingNames.APP_NODE, webi);
                request.getParams().put(BindingNames.APP_PATH, pathCl.doCall(webi));
            }
            bindings.put(CONTEXT_BINDING, request.getContextPath());
            bindings.put(REQUESTER_IP_BINDING, request.getRemoteAddr());
            bindings.put(REQUEST_BINDING, request);
            ResponseBuilder respBuilder = findResponseBuilder(serviceNode, pathElems, pathIndex, pathInfo, request);
            
            LoginService loginService = findLoginService(respBuilder);
            return new ResponseContextImpl(request, pathInfo.getBuilderPath(), pathInfo.getSubpath(), 
                    serviceNode.getNextRequestId(), loginService, respBuilder, serviceNode);
        } finally {
            if (bindings!=null)
                bindings.reset();
        }
    }
    
    private ResponseBuilder findResponseBuilder(Node node, String[] path, int pathIndex, PathInfo pathInfo, 
            Request request) 
        throws ContextUnavailableException 
    {
        ResponseBuilder respBuilder;
        try {
            respBuilder = getResponseBuilder(node, path, pathIndex, pathInfo, request);
            Node appRoot = (Node) request.getParams().get(BindingNames.APP_NODE);
            if (appRoot != null) {
                PathClosure pathCl = new PathClosure(this, request.getRootPath(), pathResolver, this);
                request.getParams().put(BindingNames.APP_PATH, pathCl.doCall(appRoot));
            }
        } catch (ContextUnavailableException ex) {
            Map<String, Object> params = new HashMap<String, Object>();
            try {
                NetworkResponseContext respContext = getContext(node, path, 0, pathInfo);
                respBuilder = new NetRespContextRespBuilderWrapper(respContext);
            } catch (ContextUnavailableException e) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().error("Response builder resolving error. "+e.getMessage());
                throw ex;
            }
        }
        return respBuilder;
    }
    
    private LoginService findLoginService(ResponseBuilder respBuilder) throws AccessDeniedException {
        return respBuilder.getResponseBuilderNode() instanceof NetworkResponseContext?
            createLoginServiceForNetRespCtx((NetworkResponseContext)respBuilder.getResponseBuilderNode()) :
            getLoginService(respBuilder.getResponseBuilderNode());
    }
    
    private LoginService createLoginServiceForNetRespCtx(NetworkResponseContext respContext) {
        Authentication auth = respContext.getAuthentication();
        return auth==null? netRespAnonLoginService : new NetRespContextLoginServiceWrapper(respContext);
    }
    
    private LoginService getLoginService(Node node) throws AccessDeniedException {
        if (node==this || node instanceof WebInterfaceNode) throw new AccessDeniedException();
        else {
            NodeAttribute attr = node.getAttr(LOGIN_SERVICE_ATTR);
            Object loginService = attr.getRealValue();
            return loginService instanceof LoginService? (LoginService)loginService : getLoginService(node.getParent());
        }
    }


    @Deprecated
    public Authentication getAuthentication(String context, String requesterIp) 
        throws NetworkResponseServiceExeption
    {
        try {
            bindingSupport.put(CONTEXT_BINDING, context);
            bindingSupport.put(REQUESTER_IP_BINDING, requesterIp);
            return getContext(this, splitContextToPathElements(context), 0, null).getAuthentication();
        } finally {
            bindingSupport.reset();
        }
    }
    
    @Deprecated
    public Response getResponse(String context, String requesterIp, Map<String, Object> params)
            throws NetworkResponseServiceExeption
    {
        long requestId = requestsCount.incrementAndGet();
        NetworkResponseContext contextNode = null;
        if (isLogLevelEnabled(LogLevel.DEBUG)) {
            String requestInfo = String.format(
                    "[%d] Processing request. " +
                    "Remote address (%s), context (%s), request parameters: %s"
                    , requestId, requesterIp, context, paramsToString(params));
            debug(requestInfo);
        }
        try {
            try {
                bindingSupport.put(REQUESTER_IP_BINDING, requesterIp);
                bindingSupport.put(REQUEST_PARAMS_BINDING, params);
                bindingSupport.put(CONTEXT_BINDING, context);
                PathInfo pathInfo = new PathInfo();
                contextNode = getContext(this, splitContextToPathElements(context), 0, pathInfo);
                if (pathInfo.getSubpath()!=null)
                    params.put(SUBCONTEXT_PARAM, pathInfo.getSubpath());
            } finally {
                bindingSupport.reset();
            }

            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("[%d] Found context for request", requestId));
            if (contextNode.isLogLevelEnabled(LogLevel.DEBUG))
                contextNode.getLogger().debug(String.format(
                    "[%d] Processing request. " +
                    "Remote address (%s), context (%s), request parameters: %s"
                    , requestId, requesterIp, context, paramsToString(params)));
            Response response = contextNode.getResponse(requesterIp, params);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                debug(String.format("[%d] Request successfully processed", requestId));
            if (contextNode.isLogLevelEnabled(LogLevel.DEBUG))
                contextNode.getLogger().debug(
                        String.format("[%d] Request successfully processed", requestId));
            if (isLogLevelEnabled(LogLevel.TRACE))
                trace(String.format("[%d] Request response \n>>>>\n%s\n<<<<", requestId, response));
            if (contextNode.isLogLevelEnabled(LogLevel.TRACE))
                contextNode.getLogger().trace(String.format(
                        "[%d] Request response \n>>>>\n%s\n<<<<", requestId, response));
            return response;
        } catch(NetworkResponseServiceExeption e) {
            requestsWithErrors.incrementAndGet();
            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage()));
            if (contextNode!=null && contextNode.isLogLevelEnabled(LogLevel.WARN))
                contextNode.getLogger().warn(String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage()));
            throw e;
        } catch(RuntimeException e) {
            requestsWithErrors.incrementAndGet();
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage())
                    , e);
            if (contextNode!=null && contextNode.isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format(
                        "[%d] Error processing request from (%s). %s"
                        , requestId, context, e.getMessage())
                    , e);
            throw e;
        }
    }
    
    private String[] splitContextToPathElements(String context) {
        return context.split("/");
    }

    private static String paramsToString(Map<String, Object> params)
    {
        if (params==null || params.isEmpty())
            return "NO PARAMETERS";
        StringBuilder buf = new StringBuilder();
        boolean firstIteration = true;
        for (Map.Entry<String, Object> param: params.entrySet())
        {
            if (!firstIteration)
                buf.append("; ");
            buf.append(param.getKey()).append(" - (").append(param.getValue()).append(")");
            if (firstIteration)
                firstIteration = false;
        }
        return buf.toString();
    }
    
    private ResponseBuilder getResponseBuilder(Node node, String[] path, int pathIndex, PathInfo pathInfo, 
            Request request) 
        throws ContextUnavailableException
    {
        ResponseBuilder groupBuilder = null;
        if (node instanceof NetworkResponseGroupNode) {
            Node builder = node.getNode("?"+request.getMethod());
            if (builder instanceof ResponseBuilder) 
                groupBuilder = (ResponseBuilder) builder;
            if (pathIndex==path.length) {
                if (groupBuilder!=null) return groupBuilder;
                else throw new ContextUnavailableException(request, path[pathIndex-1]);                    
            }
        }
        final String pathElem = path[pathIndex];
        ContextUnavailableException notFoundEx = null;
        try {
            for (Node child: node.getEffectiveNodes()) {
                if (   child.isStarted() 
                    && (pathElem.equals(child.getName()) || addNamedParameter(child, pathElem, request))) 
                {
                    if (child.getAttr(BindingNames.APP_ROOT_ATTR)!=null)
                        request.getParams().put(BindingNames.APP_NODE, child);
                    if (child instanceof NetworkResponseGroupNode) {
                        return getResponseBuilder(child, path, pathIndex+1, pathInfo, request);
                    } else {
                        if (pathIndex!=path.length-1)
                            pathInfo.setSubpath(getContextFromPath(path, pathIndex+1));
                        pathInfo.setBuilderPath(StringUtils.join(path, "/", 0, pathIndex));
                        if (child instanceof ResponseBuilder) 
                            return (ResponseBuilder)child;
                    }
                }
            }
        } catch (ContextUnavailableException e) {
            notFoundEx = e;
        }
        if (groupBuilder==null || !groupBuilder.canHandleUnknownPath()) {
            if (notFoundEx!=null) throw notFoundEx;
            else throw new ContextUnavailableException(request, pathElem);
        } else {
            pathInfo.setSubpath(getContextFromPath(path, pathIndex));
            pathInfo.setBuilderPath(StringUtils.join(path, "/", 0, pathIndex));
            return groupBuilder;
        }
    }    
    
    private boolean addNamedParameter(Node child, String paramValue, Request request) 
            throws ContextUnavailableException 
    {
        final String name = child.getName();
        if (name.length()>2 && name.startsWith("{") && name.endsWith("}")) {
            String paramName = name.substring(1, child.getName().length()-1);
            NodeAttribute validator = child.getAttr(NAMED_PARAMETER_VALIDATOR_ATTR);
            if (validator!=null) {
                getNamedParameterValidators(request).put(paramName, validator);
            }
            Object value = applyNamedParameterPattern(child, paramValue);
            if (value!=null) {
                NodeAttribute paramType = child.getAttr(NAMED_PARAMETER_TYPE_ATTR);
                if (paramType!=null) {
                    Object clazz = paramType.getRealValue();
                    if (clazz instanceof Class) 
                        try {
                            value = converter.convert((Class)clazz, value, null);
                        } catch (TypeConverterException e) {
                            throw new ContextUnavailableException(request.getContextPath(), e);
                        }
                }
                request.getParams().put(paramName, value);
                return true;
            }
        } 
        return false;
    }
    
    private Map<String, NodeAttribute> getNamedParameterValidators(Request request) {
        Map<String, NodeAttribute> validators = (Map<String, NodeAttribute>) request.getAttrs().get(NAMED_PARAMETER_VALIDATORS_ATTR);
        if (validators==null) {
            validators = new HashMap();
            request.getAttrs().put(NAMED_PARAMETER_VALIDATORS_ATTR, validators);
        }
        return validators;
    }
    
    private String applyNamedParameterPattern(Node child, String paramValue) {
        NodeAttribute patternAttr = child.getAttr(NAMED_PARAMETER_PATTERN_ATTR);
        if (patternAttr==null)
            return paramValue;
        String pattern = patternAttr.getValue();
        if (pattern==null || pattern.isEmpty())
            return paramValue;
        Pattern regexpPattern = Pattern.compile(pattern);
        Matcher matcher = regexpPattern.matcher(paramValue);
        if (!matcher.matches())
            return null;
        if (matcher.groupCount()==0)
            return paramValue;
        return matcher.group(1);
    }
    
    @Deprecated
    private NetworkResponseContext getContext(Node node, String[] path, int pathIndex, PathInfo pathInfo) 
        throws ContextUnavailableException
    {
        if (pathIndex==path.length)
            throw new ContextUnavailableException(getContextFromPath(path, 0), path[pathIndex-1]);
        final String pathElem = path[pathIndex];
        for (Node child: node.getEffectiveNodes()) {
            if (pathElem.equals(child.getName()) && child.isStarted()) {
                if (child instanceof NetworkResponseGroupNode)
                    return getContext(child, path, pathIndex+1, pathInfo);
                else {
                    if (pathIndex!=path.length-1)
                        pathInfo.setSubpath(getContextFromPath(path, pathIndex+1));
                    pathInfo.setBuilderPath(StringUtils.join(path, "/", 0, pathIndex));
//                        if (pathIndex!=path.length-1)
//                            params.put(SUBCONTEXT_PARAM, getContextFromPath(path, pathIndex+1));
                    if (child instanceof NetworkResponseContext)
                        return (NetworkResponseContext) child;
                }
            }
        }
        throw new ContextUnavailableException(getContextFromPath(path, 0), pathElem);
    }
    
    private String getContextFromPath(String[] path, int fromIndex) {
        return StringUtils.join(path, "/", fromIndex, path.length);
    }

    private class PathInfo {
        private String builderPath;
        private String subpath;

        public String getBuilderPath() {
            return builderPath;
        }

        public void setBuilderPath(String builderPath) {
            this.builderPath = builderPath;
        }

        public String getSubpath() {
            return subpath;
        }

        public void setSubpath(String subpath) {
            this.subpath = subpath;
        }
    }
    
//    private NetworkResponseContext getContext(String context, Map<String, Object> params)
//            throws ContextUnavailableException
//    {
//        int pos = context.indexOf('/');
//        if (pos>=0) {
//            String subcontext = context.substring(pos+1);
//            context = context.substring(0, pos);
//            if (params!=null)
//                params.put(SUBCONTEXT_PARAM, subcontext);
//        }
//        NetworkResponseContext contextNode = (NetworkResponseContext) getChildren(context);
//        if (contextNode==null || !contextNode.getStatus().equals(Status.STARTED))
//           throw new ContextUnavailableException(context);
//        else
//            return contextNode;
//    }
}
