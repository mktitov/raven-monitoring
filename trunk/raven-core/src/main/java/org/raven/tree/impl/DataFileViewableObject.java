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

import org.raven.log.LogLevel;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class DataFileViewableObject implements ViewableObject
{
    public static final String OCTETSTREAM_MIMETYPE = "application/octet-stream";
    private final DataFile dataFile;
    private final Node owner;

    public DataFileViewableObject(DataFile dataFile, Node owner)
    {
        this.dataFile = dataFile;
        this.owner = owner;
    }

    public String getMimeType()
    {
        try
        {
            String mimeType = dataFile.getMimeType();
            return mimeType==null? OCTETSTREAM_MIMETYPE : mimeType;
        }
        catch (DataFileException ex)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error("Error geting mime type from data file", ex);
            return OCTETSTREAM_MIMETYPE;
        }
    }

    public Object getData()
    {
        try
        {
            return dataFile.getDataStream();
        }
        catch (DataFileException ex)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error("Error reading content", ex);
            return null;
        }
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
        try
        {
            String filename = dataFile.getFilename();
            return filename!=null? filename : "unnamed_file";
        }
        catch (DataFileException ex)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error("Error geting file name", ex);
            return "unnamed_file";
        }
    }

}
