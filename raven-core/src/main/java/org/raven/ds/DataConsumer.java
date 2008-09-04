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
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 * The node that recieves data from the one or more {@link DataSource data sources}
 * @author Mikhail Titov
 */
public interface DataConsumer extends Node
{
    /**
     * Recieves data from the data source
     * @param dataSource the data source from which data recieved
     * @param data the data
     */
    public void setData(DataSource dataSource, Object data);
    /**
     * Refreshes data.
     */
    public Object refereshData(Collection<NodeAttribute> sessionAttributes);
}
