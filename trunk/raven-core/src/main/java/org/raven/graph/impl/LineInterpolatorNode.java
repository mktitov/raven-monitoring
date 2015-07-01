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

import org.jrobin.data.LinearInterpolator;
import org.jrobin.data.Plottable;
import org.raven.annotations.Parameter;
import org.raven.graph.Interpolator;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class LineInterpolatorNode extends BaseNode implements Interpolator
{
    @Parameter(defaultValue="INTERPOLATE_LEFT")
    private LineInterpolationMethod interpolationMethod;

    public Plottable createIterpolator(long[] timestamps, double[] values) throws Exception
    {
        LinearInterpolator interpolator = new LinearInterpolator(timestamps, values);
        interpolator.setInterpolationMethod(interpolationMethod.getId());

        return interpolator;
    }

    public LineInterpolationMethod getInterpolationMethod()
    {
        return interpolationMethod;
    }

    public void setInterpolationMethod(LineInterpolationMethod interpolationMethod)
    {
        this.interpolationMethod = interpolationMethod;
    }
}
