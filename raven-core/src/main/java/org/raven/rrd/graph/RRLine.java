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
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RRGraphNode.class)
@Description("Plots requested data as a line, using specified the color and width.")
public class RRLine extends RRArea
{
    @Parameter
    @Description("The width of the line")
    private Float width = 1.0f;

    public Float getWidth()
    {
        return width;
    }

    public void setWidth(Float width)
    {
        this.width = width;
    }
}
