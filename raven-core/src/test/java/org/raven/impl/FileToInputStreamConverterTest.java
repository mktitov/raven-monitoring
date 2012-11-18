/*
 * Copyright 2012 Mikhail Titov.
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
package org.raven.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.raven.test.RavenCoreTestCase;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class FileToInputStreamConverterTest extends RavenCoreTestCase {
    private File file;
    
    @Before
    public void prepare() throws IOException {
        file = new File("target/file_to_inputstream.txt");
        if (file.exists())
            file.delete();
        FileUtils.writeStringToFile(file, "12345");
    }
    
    @Test
    public void converterTest() throws IOException {
        FileToInputStreamConverter converter = new FileToInputStreamConverter();
        InputStream res = converter.convert(file, null, null);
        assertEquals("12345", IOUtils.toString(res));
    }

    @Test
    public void serviceTest() throws IOException {
        TypeConverter converter = registry.getService(TypeConverter.class);
        InputStream res = converter.convert(InputStream.class, file, null);
        assertEquals("12345", IOUtils.toString(res));
    }
}
