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

package org.raven.rrd.data;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.ConsolidationFunction;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.constraints.NotNull;
/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRDNode.class)
//@Description("Round robin archive node")
public class RRArchive extends LeafNode
{
    public final static String ROWS_ATTRIBUTE = "rows";
    public final static String CONSOLIDATIONFUNCTION_ATTRIBUTE = "consolidationFunction";
    public final static String XFF_ATTRIBUTE = "xff";
    public final static String STEPS_ATTRIBUTE = "steps";
    
    @Parameter(defaultValue="AVERAGE")
//    @Description("Consolidation function. Valid values are 'AVERAGE', 'MIN', 'MAX' and 'LAST'")
    private ConsolidationFunction consolidationFunction;
    
    @Parameter(defaultValue="0.5")
//    @Description("X-files factor. Valid values are between 0 and 1")
    private Double xff;
    
    @Parameter(defaultValue="1")
//    @Description("Number of archive steps")
    private Integer steps;
    
    @Parameter
    @NotNull
//    @Description("Number of archive rows")
    private Integer rows;
    
    public ConsolidationFunction getConsolidationFunction()
    {
        return consolidationFunction;
    }

    public Integer getRows()
    {
        return rows;
    }

    public Integer getSteps()
    {
        return steps;
    }

    public Double getXff()
    {
        return xff;
    }

//    public Integer getIndex()
//    {
//        return index;
//    }
//
//    public void setIndex(Integer index)
//    {
//        this.index = index;
//    }
//    
    
}
