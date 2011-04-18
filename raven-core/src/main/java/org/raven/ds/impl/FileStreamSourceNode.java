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

package org.raven.ds.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextService;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.table.TableImpl;
import org.raven.tree.DataStream;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataStreamValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.tmatesoft.svn.core.internal.util.CountingInputStream;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class FileStreamSourceNode extends BaseNode implements DataSource, Viewable
{
    public final static String FILE_ATTR = "file";

    @Service
    private static UserContextService userContextService;

    @Parameter(valueHandlerType=DataStreamValueHandlerFactory.TYPE)
    private DataStream file;

    @Message
    private static String statusColumnMessage;
    @Message
    private static String transmittedBytesColumnMessage;
    @Message
    private static String transmittingStatusMessage;
    @Message
    private static String transmittedStatusMessage;

    public DataStream getFile() {
        return file;
    }

    public void setFile(DataStream file) {
        this.file = file;
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);
        if (   node==this && newValue!=null && FILE_ATTR.equals(attribute.getName())
            && Status.STARTED.equals(getStatus()) && newValue instanceof InputStream)
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Received new stream. Processing...");
            UserContext context = userContextService.getUserContext();
            ContextCountingStream stream = new ContextCountingStream((InputStream)newValue);
            if (context!=null) 
                context.getParams().put(getKey(), stream);
            DataSourceHelper.sendDataToConsumers(this, stream, new DataContextImpl());
        }
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this data source");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    String getKey()
    {
        return FileStreamSourceNode.class.getName()+"_"+getId()+"_"+file;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception 
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        UserContext context = userContextService.getUserContext();
        if (context==null)
            return null;
        ContextCountingStream stream = (ContextCountingStream) context.getParams().get(getKey());
        if (stream==null)
            return null;
        TableImpl table = new TableImpl(
                new String[]{statusColumnMessage, transmittedBytesColumnMessage});
        table.addRow(new Object[]{
            stream.isTransmitting()? transmittingStatusMessage:transmittedStatusMessage
            , stream.getTransmittedBytes()});
        ViewableObject vo = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);
        return Arrays.asList(vo);
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }

    protected class ContextCountingStream extends CountingInputStream
    {
        private AtomicBoolean transmitting = new AtomicBoolean(true);

        public ContextCountingStream(InputStream in)
        {
            super(in);
        }

        public boolean isTransmitting() {
            return transmitting.get();
        }

        private long getTransmittedBytes() {
            return getBytesRead();
        }

        @Override
        public int read() throws IOException {
            int res = super.read();
            if (res==-1)
                transmitting.set(false);
            return res;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int res = super.read(b);
            if (res==-1)
                transmitting.set(false);
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int res = super.read(b, off, len);
            if (res==-1)
                transmitting.set(false);
            return res;
        }
    }
}