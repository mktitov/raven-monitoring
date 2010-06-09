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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ActionNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class DataConsumerActionNode extends ActionNode
{
    public static final String DATA_SOURCE_ATTR = "dataSource";
    public static final String DATA_BINDING = "data";
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void prepareActionBindings(
            DataContext context, Map<String, Object> additionalBindings)
    {
        super.prepareActionBindings(context, additionalBindings);
        
        Consumer consumer = new Consumer(context);
        additionalBindings.put(DATA_BINDING, consumer.getDataList());
    }

    private class Consumer implements DataConsumer
    {
        private final List dataList = new ArrayList(512);

        public Consumer(DataContext context)
        {
            for (NodeAttribute attr: getNodeAttributes())
                if (   DATA_SOURCE_ATTR.equals(attr.getParentAttribute())
                    && !context.getSessionAttributes().containsKey(attr.getName()))
                {
                    context.addSessionAttribute(attr);
                }
            dataList.clear();
            dataSource.getDataImmediate(this, context);
        }

        public void setData(DataSource dataSource, Object data, DataContext context)
        {
            dataList.add(data);
        }

        public List getDataList()
        {
            return dataList;
        }

        public Object refereshData(Collection<NodeAttribute> sessionAttributes)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getPath()
        {
            return DataConsumerActionNode.this.getPath();
        }
    }
}
