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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class FileNode extends BaseNode implements Viewable, ViewableObject
{
    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile file;

    public DataFile getFile()
    {
        return file;
    }

    public void setFile(DataFile file)
    {
        this.file = file;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        if (!getStatus().equals(Status.STARTED))
            return null;

        return Arrays.asList((ViewableObject)this);
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public String getMimeType()
    {
        try
        {
            return file.getMimeType();
        }
        catch (DataFileException ex)
        {
            error("Error getting mime type of the file from viewable object", ex);
            return null;
        }
    }

    public Object getData()
    {
        try
        {
            return file.getDataStream();
        }
        catch (DataFileException ex)
        {
            error("Error getting data of the file from viewable object", ex);
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
            return file.getFilename();
        }
        catch (DataFileException ex)
        {
            error("Error getting filename of the file from viewable object", ex);
            return null;
        }
    }
}
