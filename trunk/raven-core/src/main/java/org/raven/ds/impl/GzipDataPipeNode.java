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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.cache.TemporaryFileManager;
import org.raven.cache.TemporaryFileManagerValueHandlerFactory;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class GzipDataPipeNode extends AbstractSafeDataPipe {
    @NotNull @Parameter(valueHandlerType=TemporaryFileManagerValueHandlerFactory.TYPE)
    private TemporaryFileManager temporaryFileManager;
    
    @NotNull @Parameter(defaultValue = "utf-8")
    private Charset charsetForStringData;

    private AtomicInteger seq;
    
    @Override
    protected void initFields() {
        super.initFields();
        seq = new AtomicInteger(1);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data!=null) {
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(SKIP_DATA_BINDING, SKIP_DATA);
            bindingSupport.put(DATASOURCE_BINDING, dataSource);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            bindingSupport.put(DATA_STREAM_BINDING, new DataStreamImpl(this, context));
            try {
                InputStream stream = converter.convert(InputStream.class, data, charsetForStringData.name());
                String key = getId()+"-"+seq.getAndIncrement();
                File file = temporaryFileManager.createFile(this, key, "application/x-gzip");
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    GZIPOutputStream gzip = new GZIPOutputStream(fout);
                    try {
                        IOUtils.copy(stream, gzip);
                        data = temporaryFileManager.getDataSource(key);
                    } finally {
                        gzip.close();
                    }
                } finally {
                    IOUtils.closeQuietly(fout);
                }                
            } finally {
                bindingSupport.reset();
            }
        }
        sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, 
            BindingSupport bindingSupport) 
    {
    }

    public Charset getCharsetForStringData() {
        return charsetForStringData;
    }

    public void setCharsetForStringData(Charset charsetForStringData) {
        this.charsetForStringData = charsetForStringData;
    }

    public TemporaryFileManager getTemporaryFileManager() {
        return temporaryFileManager;
    }

    public void setTemporaryFileManager(TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }
}
