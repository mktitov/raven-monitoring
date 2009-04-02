/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.graph.impl;

import org.raven.annotations.Parameter;
import org.raven.graph.Element;
import org.raven.graph.GraphColor;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ElementNode implements Element
{
    @Parameter()
    @NotNull
    private GraphColor color;

    @Parameter()
    @NotNull()
    private String legend;

    public GraphColor getColor()
    {
        return color;
    }

    public void setColor(GraphColor color)
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
