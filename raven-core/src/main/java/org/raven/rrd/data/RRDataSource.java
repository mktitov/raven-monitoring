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
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRDNode.class)
@Description("Round robin database data source node")
public class RRDataSource extends DataPipeImpl
{
    @Parameter 
    @Description("The data source type (GAUGE | COUNTER | DERIVE | ABSOLUTE)")
    private String dataSourceType = "GAUGE";
    @Parameter
    @Description("The data source heartbeat")
    private Long heartbeat;
    @Parameter
    @Description("Minimal acceptable value")
    private double minValue = Double.NaN;
    @Parameter
    @Description("Maximal acceptable value")
    private double maxValue = Double.NaN;

    public String getDataSourceType()
    {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType)
    {
        this.dataSourceType = dataSourceType;
    }

    public Long getHeartbeat()
    {
        return heartbeat;
    }

    public void setHeartbeat(Long heartbeat)
    {
        this.heartbeat = heartbeat;
    }

    public double getMaxValue()
    {
        return maxValue;
    }

    public void setMaxValue(double maxValue)
    {
        this.maxValue = maxValue;
    }

    public double getMinValue()
    {
        return minValue;
    }

    public void setMinValue(double minValue)
    {
        this.minValue = minValue;
    }
}
