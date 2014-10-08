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
import groovy.util.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.raven.BindingNames;
import org.raven.RavenRuntimeException;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.expr.BindingSupport;
import org.raven.expr.ExpressionInfo;
import org.raven.expr.impl.BindingSupportImpl;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_SOURCES_BINDINS;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_BINDING;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_INITIATED_BINDING;
import org.raven.expr.impl.GroovyExpressionException;
import org.raven.expr.impl.GroovyExpressionExceptionAnalyzator;
import org.raven.expr.impl.RavenScriptTemplateEngine;
import org.raven.expr.impl.RavenScriptTemplateEngine.RavenTemplate;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.ContentTransformer;
import org.raven.net.NetworkResponseService;
import org.raven.net.Outputable;
import org.raven.net.ResponseContext;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.PropagatedAttributeValueError;
import org.raven.tree.Tree;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.raven.tree.impl.LoggerHelper;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class FileResponseBuilder extends AbstractResponseBuilder implements Viewable {
    public final static String FILE_ATTR = "file";
    public final static String GSP_MIME_TYPE = "text/gsp";
    public static final String TEMPLATE_BODIES_BINDING = "template_bodies";
    public static final String TEMPLATE_SOURCES_BINDING = "template_sources";
    public static final String ENTRY_TEMPLATE_CLASS_NAME = "ENTRY_TEMPLATE_CLASS_NAME";


    private final static RavenTemplate EMPTY_TEMPLATE = createEmptyTemplate();
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
    
    @Parameter(valueHandlerType = SystemSchedulerValueHandlerFactory.TYPE)
    private ExecutorService executor;
    
    @Parameter
    private Long lastModified;
    
    private AtomicReference<RavenTemplate> template;
    
    private static RavenTemplate createEmptyTemplate() {
        try {
            return (RavenTemplate) new RavenScriptTemplateEngine().createTemplate("");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void initFields() {
        super.initFields();
        template = new AtomicReference<RavenTemplate>();
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
        List<ContentTransformer> transformers = NodeUtils.getChildsOfType(this, ContentTransformer.class);
        Charset charset = null;
        if (!transformers.isEmpty())
            charset = transformers.get(transformers.size()-1).getResultCharset();
        return charset!=null? charset : file.getEncoding();
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception {
        Bindings bindings = new SimpleBindings();
        formExpressionBindings(bindings);
        return buildResponseContent(bindings);
    }
    
    public Object buildResponseContent(final Map bindings) throws Exception {        
        Object result = null;
        boolean needTransform = true;
        final boolean debugEnabled = isLogLevelEnabled(LogLevel.DEBUG);        
        if (!GSP_MIME_TYPE.equals(file.getMimeType())) {
            if (debugEnabled)
                getLogger().debug("The content of the builder is not GSP template so sending content of the file as response");
            if (bindings.containsKey("template_bodies") && isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn(String.format(
                        "File response builder (%s) used as script ROOT template "
                        + "but has mime type different from (%s)", getPath(), GSP_MIME_TYPE));
            result = file;
        } else {
            if (debugEnabled)
                getLogger().debug("The content of the builder is GSP template. Generating dynamic content...");
            RavenTemplate _template = template.get();
            if (_template==null) {
                if (debugEnabled)
                    getLogger().debug("GSP template not compiled. Compiling...");
                _template = compile();
            }
            if (_template!=null) {
                if (debugEnabled)
                    getLogger().debug("Generating content from GSP template");
                getSources(bindings).put(_template.getClassFileName(), new SourceInfo());
                final FileResponseBuilder _extendsTemplate = extendsTemplate;
                final Template thisTemplate = _template;
                if (_extendsTemplate!=null) {
                    if (debugEnabled)
                        getLogger().debug("Template extends ({}) another. Composing...", _extendsTemplate);
                    final LinkedList bodies = getBodies(bindings, true);
                    bodies.push(new Closure(this) {
                        public Object doCall() {
                            return doCall(null);
                        }
                        public Object doCall(Map params) {
                            Map bodyBindings = new HashMap(bindings);
                            if (params!=null) {
//                                bodyBindings = new HashMap();
//                                bodyBindings.putAll(bindings);
                                bodyBindings.putAll(params);
                            } 
//                            else
//                                bodyBindings = bindings;
                            addBinding(bodyBindings);
                            try {
                                
                                Object res = transform(thisTemplate.make(bodyBindings), bodyBindings);
                                if (res instanceof Writable)
                                    return (Writable)res;
                                else 
                                    return convertToWritable((Outputable)res);
                            } catch (Exception e) {
                                throw new Error(e);
                            }
                        }
                    });
                    needTransform = false;
                    result = _extendsTemplate.buildResponseContent(addExtendsTemplateParams(bindings));
                } else {
                    addBinding(bindings);
                    result = new WritableWrapper(_template.make(bindings), bindings, _template.getClassFileName());
                }
            } else if (isLogLevelEnabled(LogLevel.WARN)) {
                getLogger().warn("Generated empty content");
            }
        }        
        return needTransform? transform(result, bindings) : result;
    }
    
    private Writable convertToWritable(final Outputable outputable) {
        return new Writable() {
            public Writer writeTo(Writer out) throws IOException {
                WriterOutputStream stream = null;
                Charset charset = null;
                try {
                    charset = getContentCharset();
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
                try {
                    stream = new WriterOutputStream(out, charset);
                    outputable.outputTo(stream);
                    return out;
                } finally {
                    stream.close();
                }
            }
        };
    }
    
    private Object transform(Object source, Map bindings) throws Exception {
        List<ContentTransformer> transformers = NodeUtils.getEffectiveChildsOfType(this, ContentTransformer.class);
        if (transformers.isEmpty())
            return source;
        else {
            ExecutorService _executor = executor;
            if (_executor==null)
                throw new Exception("Can't initialize content transform chain because of executor attribute not specified");
            Outputable initialSource = source instanceof Outputable? 
                    (Outputable)source : new OutputabeWrapper(source, file.getEncoding());
            
            return new TransformController(initialSource, bindings, file.getEncoding(), getContentCharset(), 
                    _executor, transformers);
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
        LinkedList bodies = (LinkedList) bindings.get(TEMPLATE_BODIES_BINDING);
        if (bodies==null && create) {
            bodies = new LinkedList();
            bindings.put(TEMPLATE_BODIES_BINDING, bodies);                        
        }                    
        return bodies;
    }
    
    private Map<String, ExpressionInfo> getSources(Map bindings) {
        Map<String, ExpressionInfo> sources = (Map<String, ExpressionInfo>) bindings.get(TEMPLATE_SOURCES_BINDING);
        if (sources==null) {
            sources = new HashMap<String, ExpressionInfo>();
            bindings.put(TEMPLATE_SOURCES_BINDING, sources);
        }
        return sources;
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

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }
    
    public RavenTemplate compile() throws Exception {
        if (!GSP_MIME_TYPE.equals(file.getMimeType()))
            return null;
        Reader reader = file.getDataReader();
        if (reader==null && extendsTemplate==null) {
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().debug("Builder does not have content nothing to compile!");
            return null;
        }
        RavenTemplate _template = reader==null? 
                EMPTY_TEMPLATE : new RavenScriptTemplateEngine().createRavenTemplate(reader);
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
    
    private class SourceInfo implements ExpressionInfo {
//        private final String templateClassFileName;

//        public SourceInfo() {
//            this.templateClassFileName = templateClassFileName;
//        }

//        public String getTemplateClassFileName() {
//            return templateClassFileName;
//        }       

        public Node getNode() {
            return FileResponseBuilder.this;
        }

        public String getAttrName() {
            return "file";
        }

        public String getSource() {
            try {
                return org.apache.commons.io.IOUtils.toString(FileResponseBuilder.this.getFile().getDataReader());
            } catch (Exception ex) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error("Error extracting content from 'file' attribute", ex);
                return "";
            }
        }
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
    
    private class OutputabeWrapper implements Outputable {
        private final Object source;
        private final Charset initialCharset;

        public OutputabeWrapper(Object source, Charset initialCharset) {
            this.source = source;
            this.initialCharset = initialCharset;
        }

        public OutputStream outputTo(final OutputStream out) throws IOException {
            if (source==null)
                return out;          
            if (source instanceof Writable) {
                OutputStreamWriter writer = new OutputStreamWriter(out, initialCharset);
                ((Writable)source).writeTo(writer);
                writer.close();
            } else if (source instanceof Outputable) {
                ((Outputable)source).outputTo(out);
            } else if (source instanceof DataFile) {
                try {
                    IOUtils.copy(((DataFile)source).getDataStream(), out);
                } catch (DataFileException ex) {
                    throw new IOException(ex);
                }
            } else
                throw new IOException("Invalid source type: "+source.getClass().getName());
            return out;
        }
    }
    
    private class TransformController implements Outputable {
        private final Outputable outputable;
        private final Map bindings;
        private final List<ContentTransformer> transformers;
        private final ExecutorService executor;
        private final Charset initialCharset;
        private final Charset finalCharset;
        private final LoggerHelper logger;

        public TransformController(Outputable outputable, Map bindings, Charset initialCharset, 
                Charset finalCharset, ExecutorService executor, List<ContentTransformer> transformers) 
        {
            this.outputable = outputable;
            this.bindings = bindings;
            this.transformers = transformers;
            this.executor = executor;
            this.initialCharset = initialCharset;
            this.finalCharset = finalCharset;
            this.logger = new LoggerHelper(FileResponseBuilder.this, "Transform controller. ");
        }

        public OutputStream outputTo(OutputStream outStream) throws IOException {
//            BindingsHolder bindingsHolder = new BindingsHolder();
            try {
                try {
                    Outputable result = outputable;
                    Charset charset = initialCharset;
                    for (ContentTransformer transformer: transformers) {
                        final Outputable source = result;
                        final PipedOutputStream out = new PipedOutputStream();
                        final PipedInputStream reader = new PipedInputStream(out);
                        final String status = String.format("Transforming content using (%s) transformer", transformer.getName());
                        executor.execute(new AbstractTask(FileResponseBuilder.this, status) {
                            @Override public void doRun() throws Exception {
//                                BindingsHolder bindingsHolder = new BindingsHolder();
                                try {
                                    source.outputTo(out);
                                } finally {
//                                    bindingsHolder.close();
                                    out.close();
                                }
                            }
                        });
                        result = transformer.transform(reader, bindings, charset);
                        Charset newCharset = transformer.getResultCharset();
                        if (newCharset!=null)
                            charset = newCharset;
                    }
                    return result.outputTo(outStream);
                } catch (Throwable e) {
                    throw new IOException(e);
                }
            } finally {
//                bindingsHolder.close();
            }
        }
        
        private BindingSupport initExpressionExecutionContext() {
            BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
            boolean varsInitiated = varsSupport.contains(RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
            if (!varsInitiated) {
                varsSupport.put(RAVEN_EXPRESSION_VARS_INITIATED_BINDING, true);
                varsSupport.put(RAVEN_EXPRESSION_VARS_BINDING, new HashMap());
                varsSupport.put(RAVEN_EXPRESSION_SOURCES_BINDINS, new HashMap());
                return varsSupport;
            } else
                return null;
        }

        @Override
        public String toString() {
            BindingSupport varsSupport = initExpressionExecutionContext();
            try {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    outputTo(out);
                    final byte[] bytes = out.toByteArray();
                    return finalCharset!=null? new String(bytes, finalCharset) : new String(bytes);
                } catch (Throwable e) {
                    if (logger.isErrorEnabled())
                        logger.error("Error converting content to string", e);
                    return "";
                }
            } finally {
                if (varsSupport!=null)
                    varsSupport.reset();
            }
        }
        
        private class BindingsHolder {
            private final String id;
            private final BindingSupport bindingsSupport;
            private final BindingSupport varsSupport;

            public BindingsHolder() {
                varsSupport = initExpressionExecutionContext();
                bindingsSupport = new BindingSupportImpl();
                id = tree.addGlobalBindings(bindingsSupport);
                bindingsSupport.putAll(bindings);
                bindingsSupport.remove(BindingNames.NODE_BINDING);
                bindingsSupport.remove(BindingNames.LOGGER_BINDING);
                bindingsSupport.remove(BindingNames.INCLUDE_BINDING);
                bindingsSupport.remove(BindingNames.PATH_BINDING);                
            }
            
            public void close() {
                if (varsSupport!=null)
                    varsSupport.reset();
                tree.removeGlobalBindings(id);                
            }
        }
    }
    
    private class WritableWrapper implements Writable {
        private final Writable writable;
        private final Map bindings;
        private final String templateClassName;
        private final boolean entryTemplate;

        public WritableWrapper(Writable writable, Map bindings, String templateClassName) {
            this.writable = writable;
            this.bindings = bindings;
            this.templateClassName = templateClassName;
            if (!bindings.containsKey(ENTRY_TEMPLATE_CLASS_NAME)) {
                entryTemplate = true;
                bindings.put(ENTRY_TEMPLATE_CLASS_NAME, templateClassName);
            } else
                entryTemplate = false;
            
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
                if (entryTemplate) {
                    try {
                        return writable.writeTo(out);
                    } catch (Throwable e) {
                        analyzeError(e);
                        return null; 
                    }
                } else
                    return writable.writeTo(out);
            } finally {
                if (varsSupport!=null)
                    varsSupport.reset();
                tree.removeGlobalBindings(bindingsId);
            }
        }
        
        private void analyzeError(Throwable e) throws RuntimeException {
            //analyzing error
//            if (!templateClassName.equals(bindings.get(ENTRY_TEMPLATE_CLASS_NAME))) {
//                if (e instanceof IOException)
//                    throw (IOException) e;
//                else if (e instanceof RuntimeException) 
//                    throw (RuntimeException) e;
//                else 
//                    throw new RuntimeException(e);
//            }
            GroovyExpressionExceptionAnalyzator a = new GroovyExpressionExceptionAnalyzator(
                    templateClassName, e, 2, true);
            String mess = String.format("Exception in @file (%s)", getPath());
            GroovyExpressionException error = new GroovyExpressionException("", e, a);
            String errMess = GroovyExpressionExceptionAnalyzator.aggregate(error, getSources(bindings));
            if (isLogLevelEnabled(LogLevel.ERROR)) {
                if (errMess==null || errMess.isEmpty())
                    getLogger().error(mess, e);
                else
                    getLogger().error(errMess, e);
            }           
            throw new RavenRuntimeException(errMess, e);            
        }
        
        private BindingSupport initExpressionExecutionContext() {
            BindingSupport varsSupport = tree.getGlobalBindings(Tree.EXPRESSION_VARS_BINDINGS);
            boolean varsInitiated = varsSupport.contains(RAVEN_EXPRESSION_VARS_INITIATED_BINDING);
            if (!varsInitiated) {
                varsSupport.put(RAVEN_EXPRESSION_VARS_INITIATED_BINDING, true);
                varsSupport.put(RAVEN_EXPRESSION_VARS_BINDING, new HashMap());
                varsSupport.put(RAVEN_EXPRESSION_SOURCES_BINDINS, new HashMap());
                return varsSupport;
            } else
                return null;
        }

        @Override
        public String toString() {
            BindingSupport varsSupport = initExpressionExecutionContext();
            try {
                if (entryTemplate)
                    try {
                        return writable.toString();
                    } catch (Throwable e) {
                        analyzeError(e);
                        return null;
                    }
                else
                    return writable.toString();
            } finally {
                if (varsSupport!=null)
                    varsSupport.reset();
            }
        }
    } 
}
