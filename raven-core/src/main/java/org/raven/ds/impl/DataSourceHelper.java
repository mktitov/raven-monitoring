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

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceHelper
{
    private DataSourceHelper() {}

    public static void sendDataToConsumers(DataSource source, Object data)
    {
        Collection<Node> childs = source.getDependentNodes();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                if (child instanceof DataConsumer && Node.Status.STARTED.equals(child.getStatus()))
                    try
                    {
                        ((DataConsumer)child).setData(source, data);
                    }
                    catch (Throwable e)
                    {
                        if (source.isLogLevelEnabled(LogLevel.ERROR))
                            source.getLogger().error(
                                    String.format("Error pushing data to the consumer (%s)", child.getPath())
                                    , e);
                    }
    }
}
