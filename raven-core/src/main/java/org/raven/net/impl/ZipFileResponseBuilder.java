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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.activation.DataSource;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.raven.MimeTypeService;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.cache.TemporaryFileManager;
import org.raven.log.LogLevel;
import org.raven.net.ContextUnavailableException;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
//import static org.raven.net.impl.FileResponseBuilder.FILE_ATTR;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class ZipFileResponseBuilder extends AbstractResponseBuilder implements Viewable {
    public final static String FILE_ATTR = "file";
    
    @Service
    private static MimeTypeService mimeTypeService;
    
    @NotNull @Parameter(valueHandlerType = DataFileValueHandlerFactory.TYPE)
    private DataFile file;
    
    @Parameter
    private Charset zipFilenamesCharset;
    
    @Parameter
    private String baseDir;
    
    @Parameter
    private Charset defaultFileContentCharset;
    
    @Parameter
    private Long lastModified;        
    
    @Parameter
    private TemporaryFileManager temporaryFileManager;
    
    private ReentrantLock tempFileLock;

    @Override
    protected void initFields() {
        super.initFields();
        tempFileLock = new ReentrantLock();
    }

    @Override
    protected Long doGetLastModified() throws Exception {
        return lastModified;
    }

    @Override
    protected String getContentType() {
        throw new UnsupportedOperationException("Unsupported operation for this builder");
    }

    @Override
    protected Charset getContentCharset() throws Exception {
        throw new UnsupportedOperationException("Unsupported operation for this builder");
    }
    
    private InputStream getDataStreamFromTempFile(String filename, String contextPath, 
            TemporaryFileManager tempManager) 
        throws Exception 
    {
        DataFile _file = file;
        String key = getId()+"_"+_file.getChecksum();
        DataSource tempFile = tempManager.getDataSource(key);
        if (tempFile==null)
            tempFile = createTempFile(key, contextPath, tempManager);
        else if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Found zip arhive in temporary file manager");
        return tempFile.getInputStream();
    }
    
    private DataSource createTempFile(String key, String contextPath, TemporaryFileManager tempManager) throws Exception {
        if (tempFileLock.tryLock(60, TimeUnit.SECONDS)) 
            try {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug("Creating temporary file for zip archive");
                DataSource tempFile = tempManager.getDataSource(key);
                if (tempFile!=null)
                    return tempFile;
                return tempManager.saveFile(this, key, getDataStream(contextPath), "application/zip", true);
            } finally {
                tempFileLock.unlock();
            }
        throw new ContextUnavailableException("Timeout for wating while arhive copied to temporary file");
    }
    
    private InputStream getDataStream(String contextPath) throws Exception {
        InputStream dataStream = file.getDataStream();
        if (dataStream==null)
            throw new ContextUnavailableException(contextPath);
        return dataStream;
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext ctx) throws Exception {
//    protected Object buildResponseContent(UserContext user, ResponseContext ctx) throws Exception {
        String filename = (String) ctx.getRequest().getParams().get(NetworkResponseServiceNode.SUBCONTEXT_PARAM);
        String contextPath = ctx.getRequest().getServicePath()+"/"+ctx.getRequest().getContextPath();
        if (filename==null)
            throw new ContextUnavailableException(contextPath);
        
        TemporaryFileManager tempManager = temporaryFileManager;
        InputStream dataStream = tempManager==null? 
                getDataStream(contextPath) : 
                getDataStreamFromTempFile(filename, contextPath, tempManager);
        
        String _baseDir = baseDir;
        if (_baseDir != null && !baseDir.isEmpty()) {
            filename = baseDir + (baseDir.endsWith("/")? "" : "/") + filename;
        }        
        if (tempManager==null) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Searching for file {} in archive directly from node data stream", filename);
            synchronized(this) {
                return createResponse(ctx, filename, contextPath, dataStream);
            }
        } else {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Searching for file {} in archive from temporary file", filename);
            return createResponse(ctx, filename, contextPath, dataStream);
        }
        
//        final ZipArchiveInputStream stream = charset==null? 
//                    new ZipArchiveInputStream(dataStream) :
//                    new ZipArchiveInputStream(dataStream, charset.name());
//        ArchiveEntry entry = stream.getNextEntry();
//        while (entry!=null) {
//            if (!entry.isDirectory() && filename.equals(entry.getName())) {
//                if (isLogLevelEnabled(LogLevel.DEBUG))
//                    getLogger().debug("Found file {} in zip archive", filename);
//                return new ResponseImpl(
//                        mimeTypeService.getContentType(filename), stream, ctx.getHeaders(), lastModified, 
//                        defaultFileContentCharset);
//            }
//            entry = stream.getNextEntry();
//        }
//        stream.close();
//        throw new ContextUnavailableException(String.format(
//                "Context %s not found. Not found zip entry (%s)", contextPath, filename));
////        ZipInputStream stream = charset==null? 
////                new ZipInputStream(file.getDataStream()) : 
////                new ZipInputStream(file.getDataStream(), charset); //java6 not have this constructor
//        ZipInputStream stream = new ZipInputStream(dataStream);
//        ZipEntry entry = stream.getNextEntry();
//        while (entry!=null) {
//            if (!entry.isDirectory() && filename.equals(entry.getName())) {
//                if (isLogLevelEnabled(LogLevel.DEBUG))
//                    getLogger().debug("Found file {} in zip archive", filename);
//                return new ResponseImpl(
//                        mimeTypeService.getContentType(filename), stream, ctx.getHeaders(), lastModified, 
//                        defaultFileContentCharset);
//            }
//            entry = stream.getNextEntry();
//        }
//        stream.close();
//        throw new ContextUnavailableException(String.format(
//                "Context %s not found. Not found zip entry (%s)", contextPath, filename));
    }
    
    private Response createResponse(ResponseContext ctx, String filename, String contextPath, InputStream dataStream) 
            throws Exception  
    {
        Charset charset = zipFilenamesCharset;
        final ZipArchiveInputStream stream = charset==null? 
                    new ZipArchiveInputStream(dataStream) :
                    new ZipArchiveInputStream(dataStream, charset.name());
        ArchiveEntry entry = stream.getNextEntry();
        while (entry!=null) {
            if (!entry.isDirectory() && filename.equals(entry.getName())) {
                if (isLogLevelEnabled(LogLevel.DEBUG))
                    getLogger().debug("Found file {} in zip archive", filename);
                return new ResponseImpl(
                        mimeTypeService.getContentType(filename), stream, ctx.getHeaders(), lastModified, 
                        defaultFileContentCharset);
            }
            entry = stream.getNextEntry();
        }
        stream.close();
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

    public Charset getZipFilenamesCharset() {
        return zipFilenamesCharset;
    }

    public void setZipFilenamesCharset(Charset zipFilenamesCharset) {
        this.zipFilenamesCharset = zipFilenamesCharset;
    }

    public Charset getDefaultFileContentCharset() {
        return defaultFileContentCharset;
    }

    public void setDefaultFileContentCharset(Charset defaultFileContentCharset) {
        this.defaultFileContentCharset = defaultFileContentCharset;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public TemporaryFileManager getTemporaryFileManager() {
        return temporaryFileManager;
    }

    public void setTemporaryFileManager(TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }
    
    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attr, Object oldValue, Object newValue) {
        super.nodeAttributeValueChanged(node, attr, oldValue, newValue);
        if (node==this && FILE_ATTR.equals(attr.getName())) 
            setLastModified(System.currentTimeMillis());
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
}
