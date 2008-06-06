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
import org.raven.rrd.RRColor;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRGraphNode.class)
@Description(
    "Plots requested data in the form of the filled area starting" +
    " from zero, using the color specified.")
public class RRArea extends LeafNode
{
    @Parameter
    @NotNull
    @Description("The reference to the data defenition node")
    private DataDefinition dataDefinition;
    
    @Parameter()
    @Description("The color of the line")
    @NotNull
    private RRColor color;
    
    @Parameter
    @Description("The legend of the line")
    @NotNull
    private String legend;
    
    public DataDefinition getDataDefinition()
    {
        return dataDefinition;
    }

    public void setDataDefinition(DataDefinition dataDefinition)
    {
        this.dataDefinition = dataDefinition;
    }

    public RRColor getColor()
    {
        return color;
    }

    public void setColor(RRColor color)
    {
        this.color = color;
    }

    public String getLegend()
    {
        return legend;
    }

    public void setLegend(String legend)
    {
        this.legend = legend;
    }
}
