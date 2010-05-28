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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.sched.ExecutorService;
import org.raven.sched.ExecutorServiceException;
import org.raven.sched.Task;
import org.raven.table.TableImpl;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class FileDataSource extends BaseNode implements DataSource, Task, Viewable
{
    public final static String FILE_ATTR = "file";

    @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile file;
    
    @NotNull @Parameter 
    private ExecutorService executorService;

    private AtomicBoolean sendingData;
    private AtomicInteger bytesSended;
    private AtomicLong streamSize;
    private CountingInputStream countingStream;

    @Message
    private static String streamSizeMessage;
    @Message
    private static String sendPrecentMessage;
    @Message
    private static String sendStatusMessage;
    @Message
    private static String sentBytesMessages;
    @Message
    private static String sendingStatusMessage;
    @Message
    private static String contentMessage;

    @Override
    protected void initFields()
    {
        super.initFields();
        sendingData = new AtomicBoolean(false);
        bytesSended = new AtomicInteger();
        streamSize = new AtomicLong();
    }

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
        sendDataToConsumer(dataConsumer, false);
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

    public synchronized CountingInputStream getCountingStream()
    {
        return countingStream;
    }

    private synchronized void setCountingStream(CountingInputStream countingStream)
    {
        this.countingStream = countingStream;
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);
        if (Status.STARTED.equals(getStatus()) && FILE_ATTR.equals(attribute.getName()))
        {
            if (!sendingData.get())
            {
                try
                {
                    sendingData.set(true);
                    executorService.execute(this);
                }
                catch (ExecutorServiceException ex)
                {
                    sendingData.set(false);
                    if (isLogLevelEnabled(LogLevel.ERROR))
                        error(String.format(
                                "Error executing task using (%s) executor service"
                                , executorService.getPath()));
                }
            }
            else if (isLogLevelEnabled(LogLevel.WARN))
                warn("Can't send data because of already sending");
        }
    }

    public void run()
    {
        try
        {
            resetStatFields();
            Collection<Node> depNodes = getDependentNodes();
            if (depNodes!=null)
                for (Node dep: depNodes)
                    if (dep instanceof DataConsumer && Status.STARTED.equals(dep.getStatus()))
                        sendDataToConsumer((DataConsumer) dep, true);
        }
        finally
        {
            sendingData.set(false);
            resetStatFields();
        }
    }

    private void sendDataToConsumer(DataConsumer consumer, boolean gatherStat)
    {
        InputStream data = null;
        try
        {
            try{
                data = getFile().getDataStream();
                if (data!=null && gatherStat)
                {
                    data = new CountingInputStream(data);
                    setCountingStream((CountingInputStream)data);
                    streamSize.set(getFile().getFileSize());
                }
                consumer.setData(this, data);
            } finally {
                IOUtils.closeQuietly(data);
            }
        } catch (DataFileException ex) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(
                    String.format("Error pushing data to the consumer (%s)", consumer.getPath())
                    , ex);
        }
    }

    private void resetStatFields()
    {
        setCountingStream(null);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;

        List<ViewableObject> vos = new ArrayList<ViewableObject>(2);
        vos.add(new DataFileViewableObject(file, this));

        CountingInputStream is = getCountingStream();
        if (is!=null)
        {
            TableImpl table = new TableImpl(new String[]{sendStatusMessage, streamSizeMessage, sentBytesMessages, sendPrecentMessage});
            long bytesSent = is.getByteCount();
            table.addRow(new Object[]{sendingStatusMessage, streamSize.get(), bytesSent, (int)100*bytesSent/streamSize.get()});
            vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table, false));
        }

        return vos;
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }
}
