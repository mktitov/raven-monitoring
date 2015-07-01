/*
 *  Copyright 2009 Mikhail Titov.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.raven.MimeTypeService;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.ViewableObject;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class FileViewableObject implements ViewableObject
{
    @Service
    private static MimeTypeService mimeTypeService;
    
    private final File file;
    private final String mimeType;
    private final Node owner;

    public FileViewableObject(File file, Node owner)
    {
        this.file = file;
//        mimeType = (MimeType) MimeUtil.getMimeTypes(file).iterator().next();
        mimeType = mimeTypeService.getContentType(file.getName());
        this.owner = owner;
    }

    public FileViewableObject(File file, String mimeType, Node owner) {
        this.file = file;
        this.owner = owner;
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Object getData()
    {
        if (file.exists())
            try {
                FileInputStream fileStream = new FileInputStream(file);
                return fileStream;
            } catch (FileNotFoundException ex) {
                if (owner.isLogLevelEnabled(LogLevel.ERROR))
                    owner.getLogger().error(
                            String.format("Error reading the file (%s)", file.getAbsolutePath())
                            , ex);
            }
        return null;
    }

    public boolean cacheData()
    {
        return false;
    }

    public int getWidth()
    {
        return 0;
    }

    public int getHeight()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return file.getName();
    }

}
