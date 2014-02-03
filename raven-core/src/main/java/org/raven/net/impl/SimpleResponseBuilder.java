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
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.NetworkResponseService;
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
            return responseContent;
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
    
    private PathClosure createPathClosure(ResponseContext responseContext) {
        return new PathClosure(this, responseContext.getRequest().getRootPath(), pathResolver, 
                    responseService.getNetworkResponseServiceNode());
    }
    
    private Closure createRedirectClosure(final ResponseContext responseContext) {
        return new Closure(this) {
            public Object doCall(String path) {
                return new RedirectResultImpl(path);
            }
            public Object doCall(Node path) {
                PathClosure pathBuilder = createPathClosure(responseContext);
                return new RedirectResultImpl(pathBuilder.doCall(path));
            }
        };
    }
    
    private Closure createRenderClosure() {
        return new Closure(this) {
            public Object doCall(FileResponseBuilder builder) throws Throwable {
                return doCall(builder, null);
            }
            public Object doCall(FileResponseBuilder builder, Map params) throws Throwable {
                try {
                    if (!builder.isGrooveTemplate()) 
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
        };
    }
}
