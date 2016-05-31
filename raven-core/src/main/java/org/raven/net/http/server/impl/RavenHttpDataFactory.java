/*
 * Copyright 2016 Mikhail Titov.
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
package org.raven.net.http.server.impl;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.util.internal.PlatformDependent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Mikhail Titov
 */
public class RavenHttpDataFactory extends DefaultHttpDataFactory {
    private final String uploadedFilesTempDir;
    private final ConcurrentMap<HttpRequest, List<FileParam>> filesToDelete = PlatformDependent.newConcurrentHashMap();

    public RavenHttpDataFactory(final String uploadedFilesTempDir) {
        this.uploadedFilesTempDir = uploadedFilesTempDir;
    }

    @Override
    public FileUpload createFileUpload(HttpRequest request, String name, String filename, String contentType, 
            String contentTransferEncoding, Charset charset, long size) 
    {
        FileParam fileParam = new FileParam(name, filename, contentType, contentTransferEncoding, charset, size);
        getList(request).add(fileParam);
        return fileParam;
    }    

    @Override
    public void cleanAllHttpDatas() {
        super.cleanAllHttpDatas();
        for (Iterator<Map.Entry<HttpRequest, List<FileParam>>> it = filesToDelete.entrySet().iterator(); it.hasNext();){
            Map.Entry<HttpRequest, List<FileParam>> entry = it.next();
            it.remove();
            deleteFiles(entry.getValue());
        }
    }

    @Override
    public void cleanRequestHttpDatas(HttpRequest request) {
        super.cleanRequestHttpDatas(request);
        deleteFiles(filesToDelete.remove(request));
    }
    
    private void deleteFiles(List<FileParam> fileParams) {
        if (fileParams!=null && !fileParams.isEmpty()) {
            for (FileParam fileParam: fileParams)
                fileParam.delete();
            fileParams.clear();
        }
    }
    
    private List<FileParam> getList(HttpRequest request) {
        List<FileParam> list = filesToDelete.get(request);
        if (list == null) {
            list = new ArrayList<FileParam>();
            filesToDelete.put(request, list);
        }
        return list;
    }
    
    private class FileParam extends DiskFileUpload {

        public FileParam(String name, String filename, String contentType, String contentTransferEncoding, Charset charset, long size) {
            super(name, filename, contentType, contentTransferEncoding, charset, size);
        }

        @Override
        protected String getBaseDirectory() {
            return uploadedFilesTempDir;
        }
    }
}
