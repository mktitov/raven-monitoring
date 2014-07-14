/*
 * Copyright 2014 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.raven.net.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.raven.annotations.NodeClass;
import org.raven.auth.UserContext;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class EventSource extends AbstractResponseBuilder implements DataConsumer {
    
    private final static Charset UTF8 = Charset.forName("utf-8");
    private List<Channel> channels;
    private AtomicInteger asyncUsageDetector;

    @Override
    protected void initFields() {
        super.initFields();
        channels = null;
        asyncUsageDetector = null;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        channels = new CopyOnWriteArrayList<Channel>();
        asyncUsageDetector = new AtomicInteger();
    }

    @Override
    protected void doStop() throws Exception {
        for (Channel channel: channels)
            channel.responseContext.closeChannel();
        super.doStop();        
    }       

    @Override
    protected Long doGetLastModified() throws Exception {
        return null;
    }

    @Override
    protected String getContentType() {
        return "text/event-stream";
    }

    @Override
    protected Charset getContentCharset() throws Exception {
        return UTF8;
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception {
        return Response.MANAGING_BY_BUILDER;
    }

    public void setData(DataSource dataSource, Object data, DataContext context) {        
        if (data==null) 
            return;
        final int counter = asyncUsageDetector.incrementAndGet();
        try {                
            String message = "data: "+converter.convert(String.class, data, null);
            if (logger.isDebugEnabled())
                getLogger().debug("Received data for sending for submitting to channels");
            if (counter>0) {
                if (logger.isErrorEnabled())
                    getLogger().error(String.format("Async write to the channels detected. Message ignored-> ", message));
                return;
            }
            if (logger.isTraceEnabled())
                getLogger().trace("Sending message to channels -> "+message);
            byte[] messBytes = (message+"\n\n").getBytes(UTF8);
            for (Channel channel: channels)
                try {
                    channel.responseContext.getResponseStream().write(messBytes);
                } catch (IOException e) {
                    if (logger.isErrorEnabled())
                        getLogger().error(String.format("Error writing to channel (%s)", channel), e);
                }
        } finally {
            asyncUsageDetector.decrementAndGet();
        }
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private class Channel {
        private final UserContext user;
        private final ResponseContext responseContext;

        public Channel(UserContext user, ResponseContext responseContext) {
            this.user = user;
            this.responseContext = responseContext;
        }
        
    }
}
