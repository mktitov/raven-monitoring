/*
 *  Copyright 2008 Mikhail Titov.
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
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataMultiplexer<T, R> extends DataPipeImpl
{
    private final Class dataType;

    public AbstractDataMultiplexer(Class dataType) 
    {
        this.dataType = dataType;
    }

    public abstract R multiplex(List<T> listOfData);

    @Override
    public void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        try
        {
            if (data==null)
                throw new Exception("Null primary data.");
            if (dataType!=null && !dataType.isAssignableFrom(data.getClass()))
                throw new Exception(String.format(
                        "Invalid type (%s) of the primary data. Must be (%s) type."
                        , data.getClass().getName(), dataType.getName()));
            Collection<Node> childs = getSortedChildrens();
            List<T> dataList = new ArrayList<T>();
            dataList.add((T)data);
            if (childs!=null && childs.size()>0)
            {
                for (Node child: childs)
                    if (   child != dataSource 
                        && child instanceof DataConsumer 
                        && Status.STARTED==child.getStatus())
                    {
                        DataConsumer dataConsumer = (DataConsumer)child;
                        Object secData = dataConsumer.refereshData(null);
                        if (secData!=null)
                        {
                            if (!(dataType.isAssignableFrom(secData.getClass())))
                                throw new Exception(String.format(
                                        "Invalid data type recieved from the node (%s). " +
                                        "The data type must be (%s)"
                                        , dataConsumer.getPath(), dataType.getName()));
                            dataList.add((T)secData);
                        }
                    }
            }
            this.data = multiplex(dataList);
            
            super.doSetData(dataSource, data, context);
        }
        catch(Exception e)
        {
            logger.error(String.format(
                    "Error multiplexing data in the node (%s). %s", getPath(), e.getMessage()));
        }
    }
}
