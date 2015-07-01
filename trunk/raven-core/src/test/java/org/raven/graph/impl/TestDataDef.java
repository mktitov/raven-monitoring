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

import org.jrobin.core.RrdException;
import org.jrobin.data.LinearInterpolator;
import org.raven.graph.DataDef;
import org.raven.graph.GraphData;
import org.raven.graph.GraphDataDefNode;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataDef extends BaseNode implements DataDef, GraphDataDefNode
{
    public GraphData getData(Long startTime, Long endTime)
    {
        try
        {
            long[] timestamps = 
                {startTime, startTime + 3600, startTime + 7200, startTime + 10800,
                 startTime + 14400};
            double[] values = {5., 9., 6., 11., 8.};
            LinearInterpolator line = new LinearInterpolator(timestamps, values);
//            line.setInterpolationMethod(LinearInterpolator.INTERPOLATE_LEFT);

            return new GraphDataImpl(line, startTime, startTime + 14400);
        }
        catch (RrdException ex)
        {
            throw new Error(ex);
        }
    }
}
