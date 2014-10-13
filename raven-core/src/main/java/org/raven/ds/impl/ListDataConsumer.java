/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.ds.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class ListDataConsumer implements DataConsumer {
    private final List dataList = new LinkedList();
    private final Node owner;
    private final DataContext context; 

    public ListDataConsumer(Node owner, DataContext context) {
        this.owner = owner;
        this.context = context;
    }

    public synchronized void setData(DataSource dataSource, Object data, DataContext context) {
        if (data!=null)
            dataList.add(data);
        DataSourceHelper.executeContextCallbacks(owner, context, data);
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) {
        throw new UnsupportedOperationException("Operation not supported by this consumer");
    }

    public List getDataList() {
        return dataList;
    }

    public String getPath() {
        return owner.getPath();
    }
}
