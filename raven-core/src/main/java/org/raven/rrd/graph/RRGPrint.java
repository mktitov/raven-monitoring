/*
 *  Copyright 2008 Mikhail Titov .
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

package org.raven.rrd.graph;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.ConsolidationFunction;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRGraphNode.class)
@Description("")
public class RRGPrint extends LeafNode
{
    @Parameter @NotNull @Description("The reference to the data defenition node")
    private DataDefinition dataDefinition;
    
    @Parameter @NotNull @Description("The consolidation function")
    private ConsolidationFunction consolidationFunction;
    
    @Parameter @NotNull @Description("Format string (like \"average = %10.3f %s\")")
    private String format;

    public ConsolidationFunction getConsolidationFunction()
    {
        return consolidationFunction;
    }

    public void setConsolidationFunction(ConsolidationFunction consolidationFunction)
    {
        this.consolidationFunction = consolidationFunction;
    }

    public DataDefinition getDataDefinition()
    {
        return dataDefinition;
    }

    public void setDataDefinition(DataDefinition dataDefinition)
    {
        this.dataDefinition = dataDefinition;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }
}
