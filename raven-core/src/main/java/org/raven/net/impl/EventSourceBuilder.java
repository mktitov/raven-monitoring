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

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.ds.impl.DataSourceHelper;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.EventSourceChannel;
import org.raven.net.Request;
import org.raven.net.Response;
import org.raven.net.ResponseContext;
import org.raven.net.ResponseContextListener;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode = NetworkResponseServiceNode.class)
public class EventSourceBuilder extends AbstractResponseBuilder 
    implements DataPipe, ResponseContextListener,  Viewable
{
    
    public final static Charset UTF8 = Charset.forName("utf-8"); //standard for text/event-stream
    public final static String CLOSE_CHANNEL_EVENT = "CLOSE_CHANNEL";
    public static final String MESSAGE_KEY = "message";
    public final static String CHANNEL_SELECTOR_ATTR = "channelSelector";
    
    @NotNull @Parameter(valueHandlerType = NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;
    
    @Parameter(valueHandlerType = ScriptAttributeValueHandlerFactory.TYPE)
    private Boolean channelSelector;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useChannelSelector;
    
    private Collection<Channel> channels;
    private AtomicInteger asyncUsageDetector;
    private AtomicLong totalChannelsCreated;
    private AtomicLong totalMessagesSent;
    private AtomicLong totalMessagesReceivedForDelivery;
    private AtomicLong totalSendErrors;
    
    @Message private static String channelMessage;
    @Message private static String createdMessage;
    @Message private static String messagesSentMessage;
    

    @Override
    protected void initFields() {
        super.initFields();
        channels = null;
        asyncUsageDetector = null;
        totalChannelsCreated = new AtomicLong();
        totalMessagesSent = new AtomicLong();
        totalMessagesReceivedForDelivery = new AtomicLong();
        totalSendErrors = new AtomicLong();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        channels = new ConcurrentLinkedQueue<Channel>();
        asyncUsageDetector = new AtomicInteger();
        totalChannelsCreated.set(0);
        totalMessagesSent.set(0);
        totalMessagesReceivedForDelivery.set(0);
        totalSendErrors.set(0);
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
    
    public Collection<EventSourceChannel> getChannels() {
        Collection<Channel> _channels = channels;
        if (_channels==null || _channels.isEmpty())
            return Collections.EMPTY_LIST;
        return new ArrayList(_channels);
    }

    @Parameter(readOnly = true)
    public long getTotalChannelsCreated() {
        return totalChannelsCreated.get();
    }

    @Parameter(readOnly = true)
    public long getTotalMessagesSent() {
        return totalMessagesSent.get();
    }

    @Parameter(readOnly = true)
    public long getTotalMessagesReceivedForDelivery() {
        return totalMessagesReceivedForDelivery.get();
    }

    public long getTotalSendErrors() {
        return totalSendErrors.get();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Boolean getChannelSelector() {
        return channelSelector;
    }

    public void setChannelSelector(Boolean channelSelector) {
        this.channelSelector = channelSelector;
    }

    public Boolean getUseChannelSelector() {
        return useChannelSelector;
    }

    public void setUseChannelSelector(Boolean useChannelSelector) {
        this.useChannelSelector = useChannelSelector;
    }

    @Override
    protected Object buildResponseContent(UserContext user, ResponseContext responseContext) throws Exception {
        Channel channel = new Channel(user, responseContext);
        channels.add(channel);
        responseContext.addListener(this);
        totalChannelsCreated.incrementAndGet();
        if (logger.isInfoEnabled())
            getLogger().info("New channel created: {}", channel);
        //TODO: Может слать событие асинхронно через executor?
        responseContext.getHeaders().put("Content-Type", getContentType());        
        responseContext.getResponseStream().flush();
        
        DataContextImpl context = new DataContextImpl();
        context.putAt(BindingNames.CHANNEL_BINDING, channel);
        DataSourceHelper.sendDataToConsumers(this, channel, context);
        return Response.MANAGING_BY_BUILDER;
    }

    //TODO: Возможно необходимо реализовать возможность отсылки сообщений конкретным каналам. Closure in context['channelSelector']?
    //TODO: По channelId? List of channelId
    //TODO: По channelForUser?
    //TODO: нужно событие по которому канал будет закрываться. Но работать событие должно совместно с channelSelector? или по channelForUser? или по channelId
    public void setData(DataSource dataSource, final Object data, DataContext context) {        
        if (data==null || !isStarted())  {
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        } 
        if (channels.isEmpty()) {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Received data for submitting but no registered channels. Ignoring...");
            DataSourceHelper.executeContextCallbacks(this, context, data);
            return;
        }
        final int counter = asyncUsageDetector.incrementAndGet();
        try {                
            Object messObj = data;
            if (data instanceof Map && ((Map)data).containsKey(MESSAGE_KEY))
                messObj = ((Map)data).get(MESSAGE_KEY);
            final String message = "data: "+converter.convert(String.class, messObj, null);
            if (logger.isDebugEnabled())
                getLogger().debug("Received data for submitting to channels");
            if (counter>1) {
                totalSendErrors.incrementAndGet();
                if (logger.isErrorEnabled())
                    getLogger().error(String.format("Async write to the channels detected. Message ignored-> ", message));
                return;
            }
            totalMessagesReceivedForDelivery.incrementAndGet();
            byte[] messBytes = (message+"\n\n").getBytes(UTF8);
            Channel messageForChannel = (Channel) context.getAt(BindingNames.CHANNEL_BINDING);            
            if (logger.isTraceEnabled()) {
                if (messageForChannel!=null)
                    getLogger().trace(String.format("Sending message to channel (%s) -> %s", messageForChannel, message));
                else
                    getLogger().trace("Sending message to channels -> "+message);
            }
            Boolean _useChannelSelector = messageForChannel==null && useChannelSelector;
            if (_useChannelSelector) {
                bindingSupport.put(BindingNames.DATA_BINDING, data);
                bindingSupport.put(BindingNames.DATA_CONTEXT_BINDING, context);
                bindingSupport.put(BindingNames.DATASOURCE_BINDING, dataSource);
            }
            try {
                final Iterator<Channel> it = channels.iterator();
                while (it.hasNext()) {
                    final Channel channel = it.next();
                    try {
                        if (   (messageForChannel==null && selectChannel(_useChannelSelector, channel))
                            || messageForChannel==channel) 
                        {
                            final OutputStream stream = channel.responseContext.getResponseStream();
                            stream.write(messBytes);
                            stream.flush();
                            totalMessagesSent.incrementAndGet();
                            channel.messagesSent.incrementAndGet();
                            if (messageForChannel!=null)
                                break;
                        }
                    } catch (Exception e) {
                        totalSendErrors.incrementAndGet();
                        if (logger.isErrorEnabled())
                            getLogger().error(String.format("Error writing to channel (%s). May be channel closed?. Unregistering...", channel), e);
                        it.remove();
                    }
                }
            } finally {
                if (_useChannelSelector)
                    bindingSupport.reset();
            }
        } finally {
            asyncUsageDetector.decrementAndGet();
            DataSourceHelper.executeContextCallbacks(this, context, data);
        }
    }
    
    private boolean selectChannel(boolean useSelector, Channel channel) throws Exception {
        if (!useSelector)
            return true;
        bindingSupport.put(BindingNames.CHANNEL_BINDING, channel);
        Boolean res = channelSelector;
        if (res==null)
            throw new Exception("Attribute channelSelector returns null, but must return true or false");
        return res;
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        //TODO: Необходимо нарисовать таблицу со списком каналов (время создания/id канала/хост/порт/latency(как реализовать?))
        return null;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context) {
        throw new UnsupportedOperationException("EventSourceBuilder not supports pull data operations");
    }

    public Boolean getStopProcessingOnError() {
        return false;
    }

    public Collection<NodeAttribute> generateAttributes() {
        return null;
    }

    public void contextClosed(ResponseContext context) {
        Collection<Channel> _channels = channels;
        if (_channels!=null) {
            Iterator<Channel> it = _channels.iterator();
            while (it.hasNext()) {
                final Channel channel = it.next();
                if (channel.responseContext==context) {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        getLogger().debug("Channel ({}) was closed by servlet. Removing it from channel list", channel);
                    it.remove();
                }
            }
        }
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        ArrayList<ViewableObject> vos = new ArrayList<ViewableObject>(2);
        TableImpl table = new TableImpl(new String[]{channelMessage, createdMessage, messagesSentMessage});
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        for (Channel channel: channels)
            table.addRow(new Object[]{channel.toString(), fmt.format(new Date(channel.created)), channel.messagesSent});        
        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));
        return vos;
    }

    public Boolean getAutoRefresh() {
        return Boolean.TRUE;
    }
    
    private static class Channel implements EventSourceChannel {
        private final UserContext user;
        private final ResponseContext responseContext;
        private final long created = System.currentTimeMillis();
        private final AtomicLong messagesSent = new AtomicLong();

        public Channel(UserContext user, ResponseContext responseContext) {
            this.user = user;
            this.responseContext = responseContext;
        }

        @Override
        public String toString() {
            return user.getLogin()+", host-"+responseContext.getRequest().getRemoteAddr()+":"+responseContext.getRequest().getRemotePort();
        }

        public UserContext getUser() {
            return user;
        }

        public Request getRequest() {
            return responseContext.getRequest();
        }
    }
}
