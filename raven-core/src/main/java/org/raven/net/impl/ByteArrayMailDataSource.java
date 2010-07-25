/*
 *  Copyright 2010 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.net.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;

/**
 *
 * @author Mikhail Titov
 */
public class ByteArrayMailDataSource implements DataSource
{
    private final byte[] bytes;
    private final String contentType;
    private final String name;

    public ByteArrayMailDataSource(byte[] bytes, String contentType, String name)
    {
        this.contentType = contentType;
        this.name = name;
        this.bytes = bytes;
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(bytes);
    }

    public OutputStream getOutputStream() throws IOException 
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }
}
