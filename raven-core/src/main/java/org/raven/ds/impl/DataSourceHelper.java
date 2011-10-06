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

import org.raven.BindingNames;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.util.NodeUtils;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceHelper
{
    private DataSourceHelper() {}

    /**
     * Sends data to all {@link Node#getDependentNodes() dependent nodes} which are in STARTED state and
     * instance of {@link DataConsumer}
     * @param source consumers of this data source receive the data
     * @param data this data will be sent to the consumers
     * @param context the data context
     */
    public static void sendDataToConsumers(DataSource source, Object data, DataContext context)
    {
        DataConsumer cons = (DataConsumer) context.getNodeParameter(source, BindingNames.CONSUMER_PARAM);
        if (cons!=null) {
            if (source.isLogLevelEnabled(LogLevel.DEBUG))
                source.getLogger().debug("Pushing data ONLY for consumer ({})", cons.getPath());
            cons.setData(source, data, context);
        } else {
            for (DataConsumer con: NodeUtils.extractNodesOfType(source.getDependentNodes(), DataConsumer.class)) {
                try{
                    if (source.isLogLevelEnabled(LogLevel.DEBUG))
                        source.getLogger().debug("Pushing data to the consumer ({})", con.getPath());
                    con.setData(source, data, context);
                }catch(Throwable e){
                    if (source.isLogLevelEnabled(LogLevel.ERROR))
                        source.getLogger().error(String.format(
                                "Error pushing data to the consumer (%s)", con.getPath())
                                , e);
                }
            }
        }
    }
}
