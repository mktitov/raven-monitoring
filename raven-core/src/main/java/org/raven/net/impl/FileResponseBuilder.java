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
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.log.LogLevel;
import org.raven.net.ResponseContext;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class FileResponseBuilder extends AbstractResponseBuilder {
    public final static String FILE_ATTR = "file";
    public final static String GSP_MIME_TYPE = "text/gsp";
    public static final String MIME_TYPE_ATTR = "file.mimeType";
    
    @NotNull @Parameter(valueHandlerType = DataFileValueHandlerFactory.TYPE)
    private DataFile file;
    
    @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    FileResponseBuilder extendsTemplate;
    
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

    @Override
    protected Long doGetLastModified() throws Exception {
//        NodeAttribute mimeTypeAttr = getAttr(MIME_TYPE_ATTR); 
        return GSP_MIME_TYPE.equals(getFile().getMimeType())? null : lastModified;
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception {
        if (!GSP_MIME_TYPE.equals(file.getMimeType()))
            return file;
        else {
            Template _template = template.get();
            if (_template==null) 
                _template = compile();
            if (_template==null) 
                return "<<EMPTY TEMPLATE>>";
            else {
                Bindings bindings = new SimpleBindings();
                formExpressionBindings(bindings);
                bindings.put(BindingNames.NODE_BINDING, this);
                bindings.put(BindingNames.LOGGER_BINDING, getLogger());
                return _template.make(bindings);
            }
        }
    }
    
    public Object buildResponseContent(final Bindings bindings) throws Exception {
        if (!GSP_MIME_TYPE.equals(file.getMimeType()))
            return file;
        else {
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
                            addBodyBinding(bindings);
                            return thisTemplate.make(bodyBindings);
                        }
                    });
                }
                bindings.put(BindingNames.NODE_BINDING, this);
                bindings.put(BindingNames.LOGGER_BINDING, getLogger());
                return _template.make(bindings);
            }
        }        
    }
    
    private LinkedList getBodies(Bindings bindings, boolean create) {
        LinkedList bodies = (LinkedList) bindings.get("template_bodies");
        if (bodies==null && create) {
            bodies = new LinkedList();
            bindings.put("template_bodies", bodies);                        
        }                    
        return bodies;
    }
    
    private void addBodyBinding(Bindings bindings) {
        LinkedList bodies = getBodies(bindings, false);
        if (bodies != null) {
            Object body = bodies.pop();
            if (body!=null)
                bindings.put("body", body);
        }
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
    
    public Template getResponseTemplate() {
        return template.get();
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
}
