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

import org.raven.annotations.Parameter;
import org.raven.rrd.ConsolidationFunction;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RrdArchiveDefNode extends BaseNode
{
    @Parameter(defaultValue="AVERAGE")
    private ConsolidationFunction consolidationFunction;

    @Parameter(defaultValue="0.5")
    private Double xff;

    @Parameter(defaultValue="1")
    private Integer steps;

    @Parameter
    @NotNull
    private Integer rows;

	public ConsolidationFunction getConsolidationFunction()
	{
		return consolidationFunction;
	}

	public void setConsolidationFunction(ConsolidationFunction consolidationFunction)
	{
		this.consolidationFunction = consolidationFunction;
	}

	public Integer getRows()
	{
		return rows;
	}

	public void setRows(Integer rows)
	{
		this.rows = rows;
	}

	public Integer getSteps()
	{
		return steps;
	}

	public void setSteps(Integer steps)
	{
		this.steps = steps;
	}

	public Double getXff()
	{
		return xff;
	}

	public void setXff(Double xff)
	{
		this.xff = xff;
	}
}
