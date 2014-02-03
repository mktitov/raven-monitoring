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

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.MimetypesFileTypeMap;
import org.apache.commons.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class ZipFileResponseBuilderTest extends Assert {
    
    @Test
    public void test() throws Exception {
        File zipFile = new File("src/test/conf/test.zip");
        assertTrue(zipFile.exists());
        ZipInputStream stream = new ZipInputStream(new FileInputStream(zipFile), null);
        ZipEntry entry = stream.getNextEntry();
        while (entry!=null) {
            System.out.println("entry: "+entry.getName());
            if (entry.getName().equals("1")) {
                String content = IOUtils.toString(stream);
                System.out.println("content: "+content);
            }
            entry = stream.getNextEntry();
        }
    }
    
    @Test
    public void  mimeTypeTest() throws Exception {
        MimetypesFileTypeMap types = new MimetypesFileTypeMap();
        System.out.println("png: "+types.getContentType("folder/test.png"));
        System.out.println("PNG: "+types.getContentType("folder/test.PNG"));
        System.out.println("gif: "+types.getContentType("folder/test.Gif"));
        System.out.println("javascript: "+types.getContentType("folder/test.js"));
        
        MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, "test.js");

        System.out.println("tika jpg: "+mimeTypes.detect(null, metadata).toString());
    }
}
