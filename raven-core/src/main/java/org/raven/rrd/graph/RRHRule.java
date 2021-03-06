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
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRGraphNode.class)
//@Description("Draws a horizontal rule into the graph and optionally adds a legend")
public class RRHRule extends LeafNode
{
    public final static String VALUE_ATTRIBUTE = "value";
    public final static String COLOR_ATTRIBUTE = "color";
    public final static String LEGEND_ATTRIBUTE = "legend";
    public final static String WIDTH_ATTRIBUTE = "width";
    
    @Parameter @NotNull
//    @Description("Position of the rule")
    private Double value;
    
    @Parameter @NotNull
//    @Description("The color of the rule")
    private RRColor color;
    
    @Parameter
//    @Description("Legend text. If null, legend text will be omitted")
    private String legend;
    
    @Parameter(defaultValue="1") 
//    @Description("Rule width")
    private float width;

    public RRColor getColor()
    {
        return color;
    }

    public String getLegend()
    {
        return legend;
    }

    public Double getValue()
    {
        return value;
    }

    public float getWidth()
    {
        return width;
    }
}
