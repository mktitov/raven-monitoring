/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.tree.impl;

import java.io.InputStream;
import org.raven.tree.UploadedFile;

/**
 *
 * @author Mikhail Titov
 */
public class UploadedFileImpl implements UploadedFile
{
    private final String mimeType;
    private final String fileName;
    private final InputStream inputStream;

    public UploadedFileImpl(String mimeType, String fileName, InputStream inputStream) {
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.inputStream = inputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getMimeType() {
        return mimeType;
    }
}
