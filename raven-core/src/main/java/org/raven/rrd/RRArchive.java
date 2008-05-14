/*
 *  Copyright 2008 tim.
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

package org.raven.rrd;

import org.raven.annotations.Parameter;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;
/**
 *
 * @author Mikhail Titov
 */
public class RRArchive extends LeafNode
{
    @Parameter
    @Description("Consolidation function. Valid values are 'AVERAGE', 'MIN', 'MAX' and 'LAST'")
    private String consolidationFunction = "AVERAGE";
    
    @Parameter
    @Description("X-files factor. Valid values are between 0 and 1")
    private double xff = 0.99;
    
    @Parameter
    @Description("Number of archive steps")
    private int steps = 1;
    
    @Parameter
    @NotNull
    @Description("Number of archive rows")
    private Integer rows;

    public String getConsolidationFunction()
    {
        return consolidationFunction;
    }

    public void setConsolidationFunction(String consolidationFunction)
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

    public int getSteps()
    {
        return steps;
    }

    public void setSteps(int steps)
    {
        this.steps = steps;
    }

    public double getXff()
    {
        return xff;
    }

    public void setXff(double xff)
    {
        this.xff = xff;
    }
    
    
}
