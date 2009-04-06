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

import org.jrobin.data.Plottable;
import org.raven.annotations.Parameter;
import org.raven.graph.DataDef;
import org.raven.graph.InterpolatorType;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class AbstractDataDef extends BaseNode implements DataDef
{
    @Parameter(defaultValue="LINE_INTERPOLATOR")
    @NotNull
    private InterpolatorType interpolatorType;

    public Plottable getData(long startTime, long endTime)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InterpolatorType getInterpolatorType()
    {
        return interpolatorType;
    }

    public void setInterpolatorType(InterpolatorType interpolatorType)
    {
        this.interpolatorType = interpolatorType;
    }

    private void generateInterpolatorNode()
    {
        
    }

}
