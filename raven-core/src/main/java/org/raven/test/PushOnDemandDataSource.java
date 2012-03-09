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

package org.raven.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class PushOnDemandDataSource extends AbstractDataSource
{
    private List dataList = new ArrayList();
    private Map<String, NodeAttribute> lastSessionAttributes;
    private DataContext lastContext;
    private Collection<NodeAttribute> consumerAttrs = new ArrayList<NodeAttribute>();
    private PushOnDemandDataSourceListener listener;

    public PushOnDemandDataSourceListener getListener() {
        return listener;
    }

    public void setListener(PushOnDemandDataSourceListener listener) {
        this.listener = listener;
    }

    public void addDataPortion(Object data)
    {
        dataList.add(data);
    }

    public void addConsumerAttribute(NodeAttribute attr)
    {
        consumerAttrs.add(attr);
    }

    public void resetData()
    {
        dataList.clear();
    }

    public Map<String, NodeAttribute> getLastSessionAttributes()
    {
        return lastSessionAttributes;
    }

    public DataContext getLastContext() {
        return lastContext;
    }

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context)
        throws Exception
    {
        this.lastSessionAttributes = context.getSessionAttributes();
        this.lastContext = context;
        
        if (listener!=null)
            listener.onGatherDataForConsumer(dataConsumer, context);
        
        for (Object data: dataList)
            dataConsumer.setData(this, data, context);

        return true;
    }

    @Override
    public Collection<NodeAttribute> generateAttributes()
    {
        return consumerAttrs.isEmpty()? null : consumerAttrs;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }
}
