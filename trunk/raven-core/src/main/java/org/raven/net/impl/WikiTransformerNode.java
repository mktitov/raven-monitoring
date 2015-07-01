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

import info.bliki.wiki.model.WikiModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.net.ContentTransformer;
import org.raven.net.NetworkResponseService;
import org.raven.net.Outputable;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = FileResponseBuilder.class)
public class WikiTransformerNode extends BaseNode implements ContentTransformer {
    
    @Service
    private static NetworkResponseService responceService;
    
    @Parameter
    private Charset resultCharset;
    
    @NotNull @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private Node imageBase;
    
    @NotNull @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private Node linkBase;

    public Outputable transform(final InputStream source, final Map bindings, final Charset charset) {
        final Charset _resultCharset = resultCharset!=null? resultCharset : charset;
        final PathClosure path = new PathClosure(
                this, (String)bindings.get(BindingNames.ROOT_PATH), pathResolver, 
                responceService.getNetworkResponseServiceNode());
        final String imageBaseUrl = path.doCall(imageBase);
        final String linkBaseUrl = path.doCall(linkBase);
        return new Outputable() {
            public OutputStream outputTo(OutputStream out) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(out, _resultCharset);
                String text = IOUtils.toString(source, charset.name());                
                WikiModel.toHtml(text, writer, imageBaseUrl+"/${image}", linkBaseUrl+"/${title}");
//                WikiModel.toHtml(text, writer);
                writer.flush();
//                writer.close();
                return out;
            }
        };
    }

    public Charset getResultCharset() {
        return resultCharset;
    }

    public void setResultCharset(Charset resultCharset) {
        this.resultCharset = resultCharset;
    }

    public Node getImageBase() {
        return imageBase;
    }

    public void setImageBase(Node imageBase) {
        this.imageBase = imageBase;
    }

    public Node getLinkBase() {
        return linkBase;
    }

    public void setLinkBase(Node linkBase) {
        this.linkBase = linkBase;
    }
}
