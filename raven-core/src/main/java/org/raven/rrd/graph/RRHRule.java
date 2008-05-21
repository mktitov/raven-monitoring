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
@NodeClass @Description("Draws a horizontal rule into the graph and optionally adds a legend")
public class RRHRule extends LeafNode
{
    @Parameter @NotNull @Description("Position of the rule")
    private Double value;
    
    @Parameter @NotNull @Description("The color of the rule")
    private RRColor color;
    
    @Parameter @Description("Legend text. If null, legend text will be omitted")
    private String legend;
    
    @Parameter @Description("Rule width")
    private float width = 1f;

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

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }

    public float getWidth()
    {
        return width;
    }

    public void setWidth(float width)
    {
        this.width = width;
    }
}
