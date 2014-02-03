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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.raven.MimeTypeService;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.net.ContextUnavailableException;
import org.raven.net.ResponseContext;
import static org.raven.net.impl.FileResponseBuilder.FILE_ATTR;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class ZipFileResponseBuilder extends AbstractResponseBuilder {
    
    @Service
    private static MimeTypeService mimeTypeService;
    
    @NotNull @Parameter(valueHandlerType = DataFileValueHandlerFactory.TYPE)
    private DataFile file;
    
    @Parameter
    private Charset zipFilenamesCharset;
    
    @Parameter
    private Long lastModified;

    @Override
    protected Long doGetLastModified() throws Exception {
        return lastModified;
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext ctx) throws Exception {
        String filename = (String) ctx.getRequest().getParams().get(NetworkResponseServiceNode.SUBCONTEXT_PARAM);
        String contextPath = ctx.getRequest().getServicePath()+"/"+ctx.getRequest().getContextPath();
        if (filename==null)
            throw new ContextUnavailableException(contextPath);
        Charset charset = zipFilenamesCharset;
        ZipInputStream stream = charset==null? new ZipInputStream(file.getDataStream()) : 
                new ZipInputStream(file.getDataStream(), charset);
        ZipEntry entry = stream.getNextEntry();
        while (entry!=null) {
            
        }
        throw new ContextUnavailableException(String.format(
                "Context %s not found. Not found zip entry (%s)", contextPath, filename));
    }
    
    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public DataFile getFile() {
        return file;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }
    
    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue, Object newValue) {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        if (node==this && FILE_ATTR.equals(attr.getName())) 
            setLastModified(System.currentTimeMillis());
    }
}
