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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
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
    protected Map<String, Object> prepareViewableObjects(
            Map<String, NodeAttribute> refreshAttributes)
    {
        Consumer consumer = new Consumer(refreshAttributes);
        Map<String, Object> bindings = new HashMap<String, Object>();
        bindings.put(DATA_BINDING, consumer.getDataList());

        return bindings;
    }

    private class Consumer implements DataConsumer
    {
        private final List dataList = new ArrayList(512);

        public Consumer(Map<String, NodeAttribute> refreshAttributes)
        {
            dataList.clear();
            dataSource.getDataImmediate(
                    this, refreshAttributes==null? null : refreshAttributes.values());
        }

        public void setData(DataSource dataSource, Object data)
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
