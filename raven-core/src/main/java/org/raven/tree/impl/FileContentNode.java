/*
 *  Copyright 2008 Mikhail Titov.
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

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class FileContentNode extends BaseNode implements DataSource
{
    @Parameter @NotNull
    private Scheduler scheduler;

    @Parameter @NotNull
    private String url;

    @Parameter @NotNull
    private String fileMask;

    @Parameter(defaultValue="\\s+")
    private String columnDelimiter;

    @Parameter(defaultValue="\\r?\\n")
    private String rowDelimiter;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean addFileNameToFirstColumn;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean removeFileAfterProcessing;

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> generateAttributes()
    {
    }


    public void executeScheduledJob()
    {
        try
        {
            FileSystemManager manager = VFS.getManager();
            
        }
        catch (FileSystemException ex)
        {
            Logger.getLogger(FileContentNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Scheduler getScheduler()
    {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public Boolean getAddFileNameToFirstColumn() {
        return addFileNameToFirstColumn;
    }

    public void setAddFileNameToFirstColumn(Boolean addFileNameToFirstColumn) {
        this.addFileNameToFirstColumn = addFileNameToFirstColumn;
    }

    public String getColumnDelimiter() {
        return columnDelimiter;
    }

    public void setColumnDelimiter(String columnDelimiter) {
        this.columnDelimiter = columnDelimiter;
    }

    public String getFileMask() {
        return fileMask;
    }

    public void setFileMask(String fileMask) {
        this.fileMask = fileMask;
    }

    public Boolean getRemoveFileAfterProcessing() {
        return removeFileAfterProcessing;
    }

    public void setRemoveFileAfterProcessing(Boolean removeFileAfterProcessing) {
        this.removeFileAfterProcessing = removeFileAfterProcessing;
    }

    public String getRowDelimiter() {
        return rowDelimiter;
    }

    public void setRowDelimiter(String rowDelimiter) {
        this.rowDelimiter = rowDelimiter;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
