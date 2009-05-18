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

import java.util.Collection;
import org.jrobin.data.LinearInterpolator;
import org.jrobin.data.Plottable;
import org.raven.graph.DataDef;
import org.raven.graph.DataDefException;
import org.raven.graph.DataSeries;
import org.raven.graph.GraphData;
import org.raven.graph.GraphDataDefNode;
import org.raven.graph.Interpolator;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractDataDef extends BaseNode implements DataDef, GraphDataDefNode
{
    public GraphData getData(Long startTime, Long endTime) throws DataDefException
    {
        try
        {
            DataSeries dataSeries = formData(startTime, endTime);

            Plottable plottable = null;

            if (dataSeries.getValuesCount()==0)
                plottable = NanPlottable.NAN_PLOTTABLE;
            else
            {
                Interpolator interpolator = null;
                Collection<Node> childs = getChildrens();
                if (childs!=null && !childs.isEmpty())
                    for (Node child: childs)
                        if (child instanceof Interpolator)
                        {
                            interpolator = (Interpolator) child;
                            break;
                        }
                if (interpolator==null)
                {
                    plottable = new LinearInterpolator(
                            dataSeries.getTimestamps(), dataSeries.getValues());
                }
                else
                    plottable = interpolator.createIterpolator(
                            dataSeries.getTimestamps(), dataSeries.getValues());
            }
            return new GraphDataImpl(
                    plottable, dataSeries.getFirstTimeStamp(), dataSeries.getLastTimeStamp());
        }
        catch(Exception e)
        {
            throw new DataDefException(e);
        }
    }

    public abstract DataSeries formData(Long startTime, Long endTime) throws Exception;
}
