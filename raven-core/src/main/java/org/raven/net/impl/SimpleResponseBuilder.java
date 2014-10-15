/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.net.impl;

import groovy.lang.Closure;
import java.nio.charset.Charset;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletResponse;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.api.impl.ApiUtils;
import org.raven.auth.UserContext;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.HttpError;
import org.raven.net.NetworkResponseService;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class SimpleResponseBuilder extends AbstractResponseBuilder {
    @Service
    private static NetworkResponseService responseService;
    
    @NotNull @Parameter
    private String responseContentType;
    
    @Parameter
    private Charset responseContentCharset;
    
    @NotNull @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Object responseContent;
    
    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext responseContext) {
        try {
            bindingSupport.put(BindingNames.PATH_BINDING, createPathClosure(responseContext));
            bindingSupport.put(BindingNames.RENDER_BINDING, createRenderClosure());
            bindingSupport.put(BindingNames.REDIRECT_BINDING, createRedirectClosure(responseContext));
            bindingSupport.put(BindingNames.RESULT_BINDING, createResultClosure());
            bindingSupport.put(BindingNames.PROPAGATE_EXPRESSION_EXCEPTION, null);
            bindingSupport.put(BindingNames.THROW_HTTP_ERROR_BINDING, createHttpErrorClosure());
            bindingSupport.put(BindingNames.SEND_DATA_ASYNC_BINDING, createSendDataAsyncClosure(responseContext));
            try {
                return responseContent;
            } catch (HttpError e) {
                return new ResultImpl(e.getStatusCode(), e.getContent(), e.getContentType());
            }
        } finally {
            bindingSupport.reset();
        }
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    @Override
    protected Long doGetLastModified() {
        return null;
    }

    @Override
    protected String getContentType() {
        return responseContentType;
    }

    @Override
    protected Charset getContentCharset() throws Exception {
        return responseContentCharset;
    }

    public Object getResponseContent() {        
        return responseContent;
    }

    public void setResponseContent(Object responseContent) {
        this.responseContent = responseContent;
    }

    public Charset getResponseContentCharset() {
        return responseContentCharset;
    }

    public void setResponseContentCharset(Charset responseContentCharset) {
        this.responseContentCharset = responseContentCharset;
    }
    
    private Closure createHttpErrorClosure() {
        return new Closure(this) {
            public Object doCall(int statusCode, String contentType, String content) {
                throw new HttpError(statusCode, contentType, content);
            }
            public Object doCall(int statusCode, String content) {
                throw new HttpError(statusCode, content);
            }
            public Object doCall(String content) {
                throw new HttpError(content);
            }
        };
    }
    
    private PathClosure createPathClosure(ResponseContext responseContext) {
        return new PathClosure(this, responseContext.getRequest().getRootPath(), pathResolver, 
                    responseService.getNetworkResponseServiceNode());
    }
    
    private Closure createRedirectClosure(final ResponseContext responseContext) {
        return new Closure(this) {
            public Object doCall(String path) {
                return new RedirectResult(path);
            }
            public Object doCall(Node path) {
                PathClosure pathBuilder = createPathClosure(responseContext);
                return new RedirectResult(pathBuilder.doCall(path));
            }
        };
    }
    
    private Closure createResultClosure() {
        return new Closure(this) {
            public Object doCall(Object content) {
                return new ResultImpl(HttpServletResponse.SC_OK, content);
            }
            public Object doCall(int status, Object content) {
                return new ResultImpl(status, content);
            }
            public Object doCall(int status, String contentType, Object content) {
                return new ResultImpl(status, content, contentType);
            }
        };
    }
    
    private Closure createRenderClosure() {
        return new Closure(this) {
            public Object doCall(FileResponseBuilder builder) throws Throwable {
                return doCall(builder, null);
            }
            
            public Object doCall(int status, FileResponseBuilder builder) throws Throwable {
                return new ResultImpl(status, doCall(builder, null));
            }
            
            public Object doCall(FileResponseBuilder builder, Map params) throws Throwable {
                try {
                    if (!builder.isGroovyTemplate()) 
                        return builder.getFile();
                    else {
                        Bindings bindings = new SimpleBindings();
                        SimpleResponseBuilder.this.formExpressionBindings(bindings);
                        if (params!=null && !params.isEmpty())
                            bindings.putAll(params);
                        return builder.buildResponseContent(bindings);
                    } 
                } catch (Throwable e) {
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        getLogger().error(String.format("Error rendering file/template (%s)", builder), e);
                    throw e;
                }
            }
            
            public Object doCall(int status, FileResponseBuilder builder, Map params) throws Throwable {
                return new ResultImpl(status, doCall(builder, params));
            }
        };
    }
    
    private Closure createSendDataAsyncClosure(final ResponseContext responseContext) {
        return new Closure(this) {
            private final ResponsePromiseImpl promise = new ResponsePromiseImpl();
            
            public Object doCall(DataSource source, DataConsumer target, Object data, Closure callback) throws Exception {
                DataContext context = new DataContextImpl();
                return doCall(source, target, context, data, callback);
            }
            
            public Object doCall(DataSource source, DataConsumer target, DataContext ctx, Object data, Closure callback) throws Exception {
                switch (callback.getMaximumNumberOfParameters()) {
                    case 0 : ctx.addCallbackOnEach(createCallback0(callback));
                    case 1 : ctx.addCallbackOnEach(createCallback1(callback));
                    case 2 : ctx.addCallbackOnEach(createCallback2(callback));
                }
                ApiUtils.sendData(source, target, ctx, data);
                return promise;
            }
            
            private Closure createCallback0(final Closure callback) {
                return new Closure(this) {
                    public Object doCall() {
                        processResult(callback);
                        return null;
                    }
                };
            }
            
            private Closure createCallback1(final Closure callback) {
                return new Closure(this) {
                    public Object doCall(Node initiator) {
                        processResult(callback, initiator);
                        return null;
                    }
                };
            }
            
            private Closure createCallback2(final Closure callback) {
                return new Closure(this) {
                    public Object doCall(Node initiator, Object data) {
                        processResult(callback, initiator, data);
                        return null;
                    }
                };
            }
            
            private void processResult(Closure callback, Object... args) {
                try {
                    Object res = callback.call(args);
                    createResponse(res);
                } catch (HttpError e) {
                    createResponse(new ResultImpl(e.getStatusCode(), e.getContent(), e.getContentType()));
                } catch (Throwable e) {
                    promise.error(e);
                }
            }
            
            private void createResponse(Object res) {
                if (res instanceof Response)
                    promise.success((Response)res);
                else
                    try {
                        promise.success(new ResponseImpl(getContentType(), res, responseContext.getHeaders(), 
                                    doGetLastModified(), getContentCharset()));
                    } catch (Exception e) {
                        promise.error(e);
                    }
            }
        };
    }
}
