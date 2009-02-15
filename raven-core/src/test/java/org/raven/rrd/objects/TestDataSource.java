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

package org.raven.rrd.objects;

import java.util.Collection;
import java.util.Map;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractThreadedDataSource;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataSource extends AbstractThreadedDataSource
{
    private double value = 1.;
    private double value2 = 100.;
    
    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        if (((Node)dataConsumer).getName().equals("ds"))
            dataConsumer.setData(this, value++);
        else
            dataConsumer.setData(this, value2--);
        System.out.println(">>>value"+value);
        return true;
    }
    
}
