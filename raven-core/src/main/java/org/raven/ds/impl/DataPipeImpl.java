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

import java.util.Collection;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 * Collects data from one data source and transmits it to all {@link DataConsumer data consumers} 
 * in the {@link org.raven.tree.Node#getDependentNodes()  dependency list}.
 * @author Mikhail Titov
 */
public class DataPipeImpl extends AbstractDataConsumer implements DataPipe
{
    public void setData(DataSource dataSource, Object data)
    {
        if (getDependentNodes()!=null)
            for (Node node: getDependentNodes())
                if (node instanceof DataConsumer)
                    ((DataConsumer)node).setData(this, data);
    }

    public void getDataImmediate(DataConsumer dataConsumer)
    {
        getDataSource().getDataImmediate(this);
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }
}
