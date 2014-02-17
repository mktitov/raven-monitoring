/*
 * Copyright 2014 Mikhail Titov.
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
import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_BINDING;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_INITIATED_BINDING;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.NetworkResponseService;
import org.raven.net.ResponseContext;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Tree;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class FileResponseBuilder extends AbstractResponseBuilder implements Viewable{
    public final static String FILE_ATTR = "file";
    public final static String GSP_MIME_TYPE = "text/gsp";
    public static final String MIME_TYPE_ATTR = "file.mimeType";
    
    @Service
    private static NetworkResponseService responceService;
    
    @NotNull @Parameter
    private String responseContentType;
    
    @NotNull @Parameter(valueHandlerType = DataFileValueHandlerFactory.TYPE)
    private DataFile file;
    
    @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private FileResponseBuilder extendsTemplate;
    
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Map extendsTemplateParams;
    
    @Parameter
    private Long lastModified;
    
    private AtomicReference<Template> template;

    @Override
    protected void initFields() {
        super.initFields();
        template = new AtomicReference<Template>();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        template.set(null);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        template.set(null);
    }
    
    public boolean isGroovyTemplate() throws Exception {
        return GSP_MIME_TYPE.equals(getFile().getMimeType());
    }

    @Override
    protected Long doGetLastModified() throws Exception {
//        NodeAttribute mimeTypeAttr = getAttr(MIME_TYPE_ATTR); 
        return GSP_MIME_TYPE.equals(getFile().getMimeType())? null : lastModified;
    }

    @Override
    protected String getContentType() {
        return responseContentType;
    }

    @Override
    protected Charset getContentCharset() throws Exception {
        return file.getEncoding();
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception {
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        return buildResponseContent(bindings);
    }
    
    public Object buildResponseContent(final Map bindings) throws Exception {        
        if (!GSP_MIME_TYPE.equals(file.getMimeType())) {
            if (bindings.containsKey("template_bodies") && isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn(String.format(
                        "File response builder (%s) used as script ROOT template "
                        + "but has mime type different from (%s)", getPath(), GSP_MIME_TYPE));
            return file;
        } else {
            Template _template = template.get();
            if (_template==null) 
                _template = compile();
            if (_template==null) 
                return "<<EMPTY TEMPLATE>>";
            else {
                final FileResponseBuilder _extendsTemplate = extendsTemplate;
                final Template thisTemplate = _template;
                if (_extendsTemplate!=null) {
                    final LinkedList bodies = getBodies(bindings, true);
                    bodies.push(new Closure(this) {
                        public Object doCall() {
                            return doCall(null);
                        }
                        public Object doCall(Map params) {
                            Map bodyBindings = null;
                            if (params!=null) {
                                bodyBindings = new HashMap();
                                bodyBindings.putAll(bindings);
                                bodyBindings.putAll(params);
                            } else
                                bodyBindings = bindings;
                            addBinding(bodyBindings);
                            return thisTemplate.make(bodyBindings);
                        }
                    });
                    return _extendsTemplate.buildResponseContent(addExtendsTemplateParams(bindings));
                } else {
                    addBinding(bindings);
                    return new WritableWrapper(_template.make(bindings), bindings);
                }
            }
        }        
    }
    
    private BindingSupport initExpressionExecutionContext() {
        BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
        boolean varsInitiated = varsSupport.contains(RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
        if (!varsInitiated) {
            varsSupport.put(RAVEN_EXPRESSION_VARS_INITIATED_BINDING, true);
            varsSupport.put(RAVEN_EXPRESSION_VARS_BINDING, new HashMap());
            return varsSupport;
        } else
            return null;
    }
    
    private Map addExtendsTemplateParams(Map bindings) {
        try {
            bindingSupport.putAll(bindings);
            Map params = extendsTemplateParams;
            if (params!=null)
                bindings.putAll(params);
            return bindings;
        } finally {
            bindingSupport.reset();
        }
    } 
    
    private LinkedList getBodies(Map bindings, boolean create) {
        LinkedList bodies = (LinkedList) bindings.get("template_bodies");
        if (bodies==null && create) {
            bodies = new LinkedList();
            bindings.put("template_bodies", bodies);                        
        }                    
        return bodies;
    }
    
    private void addBinding(Map bindings) {
        
        bindings.put(BindingNames.NODE_BINDING, this);
        bindings.put(BindingNames.LOGGER_BINDING, getLogger());
        bindings.put(BindingNames.INCLUDE_BINDING, new Include(bindings));
        bindings.put(BindingNames.PATH_BINDING, new PathClosure(
                this, (String)bindings.get(BindingNames.ROOT_PATH), pathResolver, 
                responceService.getNetworkResponseServiceNode()));
        LinkedList bodies = getBodies(bindings, false);
        if (bodies != null && !bodies.isEmpty()) 
            bindings.put("body", bodies.pop());
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }
    
    public DataFile getFile() {
        return file;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }
    
    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public FileResponseBuilder getExtendsTemplate() {
        return extendsTemplate;
    }

    public void setExtendsTemplate(FileResponseBuilder extendsTemplate) {
        this.extendsTemplate = extendsTemplate;
    }
    
    public Template getResponseTemplate() {
        return template.get();
    }

    public Map getExtendsTemplateParams() {
        return extendsTemplateParams;
    }

    public void setExtendsTemplateParams(Map extendsTemplateParams) {
        this.extendsTemplateParams = extendsTemplateParams;
    }
    
    public Template compile() throws Exception {
        if (!GSP_MIME_TYPE.equals(file.getMimeType()))
            return null;
        Reader reader = file.getDataReader();
        if (reader==null)
            return null;
        Template _template = new SimpleTemplateEngine().createTemplate(reader);
        template.set(_template);        
        return _template;
    }
    
    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue, Object newValue) {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        if (node==this && ObjectUtils.in(attr.getName(), FILE_ATTR, MIME_TYPE_ATTR)) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Reseting template...");
            template.set(null);
            if (FILE_ATTR.equals(attr.getName()))
                setLastModified(System.currentTimeMillis());
        } 
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        return Arrays.asList((ViewableObject)new DataFileViewableObject(file, this));
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }
    
    private class Include extends Closure {
        private final Map bindings;

        public Include(Map bindings) {
            super(FileResponseBuilder.this);
            this.bindings = bindings;
        }
        
        public Object doCall(FileResponseBuilder builder) throws Throwable {
            return doCall(builder, null);
        }
        
        public Object doCall(FileResponseBuilder builder, Map params) throws Throwable {
            Map includeBindings = bindings;
            if (params!=null && !params.isEmpty()) {
                includeBindings = new HashMap();
                includeBindings.putAll(bindings);
                includeBindings.putAll(params);
            }
            try {
                Object res = builder.buildResponseContent(includeBindings);
                return res instanceof DataFile? ((DataFile)res).getDataReader() : res;
            } catch (Throwable e) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(String.format("Error including file/template (%s)", builder));
                throw e;
            }
        }
    }
    
    private class WritableWrapper implements Writable {
        private final Writable writable;
        private final Map bindings;

        public WritableWrapper(Writable writable, Map bindings) {
            this.writable = writable;
            this.bindings = bindings;
        }

        public Writer writeTo(Writer out) throws IOException {
            BindingSupport varsSupport = initExpressionExecutionContext();
            final BindingSupportImpl bindingsSupport = new BindingSupportImpl();
            final String bindingsId = tree.addGlobalBindings(bindingsSupport);
            try {
                bindingsSupport.putAll(bindings);
                bindingsSupport.remove(BindingNames.NODE_BINDING);
                bindingsSupport.remove(BindingNames.LOGGER_BINDING);
                bindingsSupport.remove(BindingNames.INCLUDE_BINDING);
                bindingsSupport.remove(BindingNames.PATH_BINDING);
                return writable.writeTo(out);
            } finally {
                if (varsSupport!=null)
                    varsSupport.reset();
                tree.removeGlobalBindings(bindingsId);
            }
        }
        
        private BindingSupport initExpressionExecutionContext() {
            BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
            boolean varsInitiated = varsSupport.contains(RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
            if (!varsInitiated) {
                varsSupport.put(RAVEN_EXPRESSION_VARS_INITIATED_BINDING, true);
                varsSupport.put(RAVEN_EXPRESSION_VARS_BINDING, new HashMap());
                return varsSupport;
            } else
                return null;
        }

        @Override
        public String toString() {
            BindingSupport varsSupport = initExpressionExecutionContext();
            try {
                return writable.toString();
            } finally {
                if (varsSupport!=null)
                    varsSupport.reset();
            }
        }
        
    } 
}
