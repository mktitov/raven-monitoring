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

package org.raven.ds.impl;

import groovy.lang.Writable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.util.IOUtils;
import org.raven.expr.BindingSupport;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_BINDING;
import static org.raven.expr.impl.ExpressionAttributeValueHandler.RAVEN_EXPRESSION_VARS_INITIATED_BINDING;
import org.raven.net.ContentTransformer;
import org.raven.net.Outputable;
import org.raven.sched.ExecutorService;
import org.raven.sched.impl.AbstractTask;
import org.raven.tree.Node;
import org.raven.tree.Tree;
import org.raven.tree.impl.LoggerHelper;
import org.raven.util.NodeUtils;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class ContentTransformerController implements Outputable {
    private final Tree tree;
    private final Node owner;
    private final Outputable outputable;
    private final Map bindings;
    private final List<ContentTransformer> transformers;
    private final ExecutorService executor;
    private final Charset initialCharset;
    private final Charset finalCharset;
    private final LoggerHelper logger;
    
    public static Charset getFinalCharset(Node transformersOwner, Charset initialCharset) {
        throw new UnsupportedOperationException();
    }
    
    public static List<ContentTransformer> getContentTransformers(Node owner) {
        return NodeUtils.getChildsOfType(owner, ContentTransformer.class);
    }

    public ContentTransformerController(Tree tree, Node owner, Outputable outputable, Map bindings, 
            Charset initialCharset, Charset finalCharset, ExecutorService executor, 
            List<ContentTransformer> transformers) 
    {
        this.tree = tree;
        this.owner = owner;
        this.outputable = outputable;
        this.bindings = bindings;
        this.transformers = transformers;
        this.executor = executor;
        this.initialCharset = initialCharset;
        this.finalCharset = finalCharset;
        this.logger = new LoggerHelper(owner, "Transform controller. ");
    }

    public OutputStream outputTo(OutputStream outStream) throws IOException {
        try {
            Outputable result = outputable;
            Charset charset = initialCharset;
            for (ContentTransformer transformer: transformers) {
                final Outputable source = result;
                final PipedOutputStream out = new PipedOutputStream();
                final PipedInputStream reader = new PipedInputStream(out);
                final String status = String.format("Transforming content using (%s) transformer", transformer.getName());
                executor.execute(new AbstractTask(owner, status) {
                    @Override public void doRun() throws Exception {
                        try {
                            source.outputTo(out);
                        } finally {
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
    
    public static class OutputabeWrapper implements Outputable {
        private final Object source;
        private final Charset initialCharset;
        private final TypeConverter converter;

        public OutputabeWrapper(Object source, Charset initialCharset, TypeConverter converter) {
            this.source = source;
            this.initialCharset = initialCharset;
            this.converter = converter;
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
            } else {
                String charset = initialCharset==null? null : initialCharset.name();
                InputStream sourceStream = converter.convert(InputStream.class, source, charset);
                IOUtils.copy(sourceStream, out);
            }
            return out;
        }
    }
    

//    private class BindingsHolder {
//        private final String id;
//        private final BindingSupport bindingsSupport;
//        private final BindingSupport varsSupport;
//
//        public BindingsHolder() {
//            varsSupport = initExpressionExecutionContext();
//            bindingsSupport = new BindingSupportImpl();
//            id = tree.addGlobalBindings(bindingsSupport);
//            bindingsSupport.putAll(bindings);
//            bindingsSupport.remove(BindingNames.NODE_BINDING);
//            bindingsSupport.remove(BindingNames.LOGGER_BINDING);
//            bindingsSupport.remove(BindingNames.INCLUDE_BINDING);
//            bindingsSupport.remove(BindingNames.PATH_BINDING);                
//        }
//
//        public void close() {
//            if (varsSupport!=null)
//                varsSupport.reset();
//            tree.removeGlobalBindings(id);                
//        }
//    }
}    
