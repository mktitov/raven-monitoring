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

package org.raven.ds;

import java.util.Collection;
import org.raven.tree.NodeAttribute;
import org.raven.tree.PathObject;

/**
 * The node that receives data from the one or more {@link DataSource data sources}
 * @author Mikhail Titov
 */
public interface DataConsumer extends PathObject
{
    /**
     * Receives data from the data source
     * @param dataSource the data source from which data received
     * @param data the data
     * @param context the context of the data processing
     */
    public void setData(DataSource dataSource, Object data, DataContext context);
    /**
     * Refreshes data.
     */
    public Object refereshData(Collection<NodeAttribute> sessionAttributes);
}
