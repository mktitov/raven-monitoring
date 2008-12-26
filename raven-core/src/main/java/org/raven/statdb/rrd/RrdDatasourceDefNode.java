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

package org.raven.statdb.rrd;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.DataSourceType;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RrdDatasourceDefNode extends BaseNode
{
    @Parameter(defaultValue="GAUGE")
    private DataSourceType dataSourceType;

    @Parameter
    private Long heartbeat;

    @Parameter(defaultValue="NaN")
    private Double minValue;

    @Parameter(defaultValue="NaN")
    private Double maxValue;

	public DataSourceType getDataSourceType()
	{
		return dataSourceType;
	}

	public void setDataSourceType(DataSourceType dataSourceType)
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

	public Double getMaxValue()
	{
		return maxValue;
	}

	public void setMaxValue(Double maxValue)
	{
		this.maxValue = maxValue;
	}

	public Double getMinValue()
	{
		return minValue;
	}

	public void setMinValue(Double minValue)
	{
		this.minValue = minValue;
	}
}
