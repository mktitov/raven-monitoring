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

package org.raven.rrd.data;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.DataPipeImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRDNode.class)
@Description("Round robin database data source node")
public class RRDataSource extends DataPipeImpl
{
    public final static String DATASOURCETYPE_ATTRIBUTE = "dataSourceType";
    
    @Parameter(defaultValue="GAUGE")
    @Description("The data source type (GAUGE | COUNTER | DERIVE | ABSOLUTE)")    
    private String dataSourceType;
    
    @Parameter
    @Description("The data source heartbeat")
    private Long heartbeat;
    
    @Parameter(defaultValue="NaN")
    @Description("Minimal acceptable value")
    private Double minValue;

    @Parameter(defaultValue="NaN")
    @Description("Maximal acceptable value")
    private Double maxValue;

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue) 
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);
        
        if (   node==this 
            && newValue==null 
            && getStatus()==Status.STARTED 
            && DATASOURCE_ATTRIBUTE.equals(attribute.getName()))
        {
            setStatus(Status.INITIALIZED);
        }
    }

    public String getDataSourceType()
    {
        return dataSourceType;
    }

    public Long getHeartbeat()
    {
        return heartbeat;
    }

    public Double getMaxValue()
    {
        return maxValue;
    }

    public Double getMinValue()
    {
        return minValue;
    }
}
