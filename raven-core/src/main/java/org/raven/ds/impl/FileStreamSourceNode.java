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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.DataStream;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.DataStreamValueHandlerFactory;
import org.tmatesoft.svn.core.internal.util.CountingInputStream;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class FileStreamSourceNode extends BaseNode implements DataSource, Viewable
{
    @Parameter(valueHandlerType=DataStreamValueHandlerFactory.TYPE)
    private DataStream file;

    public DataStream getFile() {
        return file;
    }

    public void setFile(DataStream file) {
        this.file = file;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("Pull operation not supported by this data source");
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    private String getKey()
    {
        return FileStreamSourceNode.class.getName()+"_"+getId()+"_"+file;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }

    private class ContextCountingStream extends CountingInputStream
    {
        private final UserContext userContext;
        private AtomicBoolean transmitting = new AtomicBoolean(true);

        public ContextCountingStream(InputStream in, UserContext userContext)
        {
            super(in);
            this.userContext = userContext;
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
