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

import groovy.lang.Writable;
import groovy.text.Template;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.DataFile;

/**
 *
 * @author Mikhail Titov
 */
public class FileResponseBuilderTest extends RavenCoreTestCase {
    
    private final static String NODE_NAME = "file response";
    private FileResponseBuilder builder;
    
    @Before
    public void prepare() {
        builder = new FileResponseBuilder();
        builder.setName(NODE_NAME);
        testsNode.addAndSaveChildren(builder);
        builder.setResponseContentType("text/html");
    }
    
    @Test
    public void simpleFileTest() throws Exception {
        byte[] data = "test".getBytes();
        assertNull(builder.doGetLastModified());
        long curTime = System.currentTimeMillis();
        builder.getFile().setDataStream(new ByteArrayInputStream(data));
        Long lastModified = builder.doGetLastModified();
        assertNotNull(lastModified);
        assertTrue(lastModified >= curTime);
        assertTrue(builder.start());
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof DataFile);
        byte[] respData = IOUtils.toByteArray(((DataFile)resp).getDataStream());
        assertArrayEquals(data, respData);
    }
    
    @Test
    public void templateTest() throws Exception {
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        assertNull(builder.doGetLastModified());
        builder.getFile().setDataString("${node.name}");
        assertNull(builder.doGetLastModified());
        assertTrue(builder.start());
        Object resp = builder.buildResponseContent(null, null);
        assertNotNull(resp);
        assertTrue(resp instanceof Writable);
        assertEquals(NODE_NAME, resp.toString());
        Template template = builder.getResponseTemplate();
        assertNotNull(template);
        
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertSame(template, builder.getResponseTemplate());
        
        builder.getFile().setFilename("changed file name");
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertSame(template, builder.getResponseTemplate());
        
        Long lastModified = builder.getLastModified();
        builder.getFile().setMimeType("text/html");
        assertEquals(lastModified, builder.getLastModified());
        assertEquals(lastModified, builder.doGetLastModified());
        builder.getFile().setMimeType(FileResponseBuilder.GSP_MIME_TYPE);
        assertEquals(lastModified, builder.getLastModified());
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertNotSame(template, builder.getResponseTemplate());
        template = builder.getResponseTemplate();
        
        builder.getFile().setDataString("${node.name}");
        assertEquals(NODE_NAME, builder.buildResponseContent(null, null).toString());
        assertNotSame(template, builder.getResponseTemplate());
        
    }
}