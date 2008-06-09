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

package org.raven.rrd.graph;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 * This is the named source of the data for the graph. 
 * Holds the reference to the {@link org.raven.rrd.data.RRDataSource}
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRGraphNode.class)
@Description("Defines virtual datasource.")
public class RRDef extends LeafNode implements DataDefinition
{
    @Parameter()
    @Description("The reference to the rrd data source from which data will be taken")
    @NotNull
    //TODO: add RRDataSourceReferenceValues
    private RRDataSource dataSource;
    
    @Parameter
    @Description(
        "Consolidation function. If not seted consolidation function will be taken from the" +
        "rrd archive corresponding the dataSource.")
    @NotNull
    private ConsolidationFunction consolidationFunction = ConsolidationFunction.AVERAGE;

    public ConsolidationFunction getConsolidationFunction()
    {
        return consolidationFunction;
    }

    public void setConsolidationFunction(ConsolidationFunction consolidationFunction)
    {
        this.consolidationFunction = consolidationFunction;
    }

    public RRDataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(RRDataSource dataSource)
    {
        this.dataSource = dataSource;
    }
}
