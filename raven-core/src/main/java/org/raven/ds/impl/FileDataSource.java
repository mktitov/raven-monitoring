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

package org.raven.ds.impl;

import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class FileDataSource extends BaseNode implements DataSource, Task
{
    public final static String FILE_ATTR = "file";

    @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile file;
    
    @NotNull @Parameter 
    private ExecutorService executorService;

    public DataFile getFile()
    {
        return file;
    }

    public void setFile(DataFile file)
    {
        this.file = file;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        if (!Status.STARTED.equals(getStatus()))
            return false;
        sendDataToConsumer(dataConsumer);
        return true;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    public Node getTaskNode()
    {
        return this;
    }

    public String getStatusMessage()
    {
        return "Working";
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);
        if (Status.STARTED.equals(getStatus()) && FILE_ATTR.equals(attribute.getName()))
            try
            {
                executorService.execute(this);
            }
            catch (ExecutorServiceException ex)
            {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    error(String.format(
                            "Error executing task using (%s) executor service"
                            , executorService.getPath()));
            }
    }

    public void run()
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes!=null)
            for (Node dep: depNodes)
                if (dep instanceof DataConsumer && Status.STARTED.equals(dep.getStatus()))
                    sendDataToConsumer((DataConsumer) dep);
    }

    private void sendDataToConsumer(DataConsumer consumer)
    {
        InputStream data = null;
        try
        {
            data = getFile().getDataStream();
            consumer.setData(this, data);
        } catch (DataFileException ex)
        {
            IOUtils.closeQuietly(data);
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format("Error pushing data to the consumer (%s)", consumer.getPath())
                    , ex);
        }
    }
}
