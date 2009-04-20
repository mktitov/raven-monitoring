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

import org.raven.graph.DataSeries;

/**
 *
 * @author Mikhail Titov
 */
public class DataSeriesImpl implements DataSeries
{
    public final static DataSeries EMPTY_DATA_SERIES =
            new DataSeriesImpl(new long[]{}, new double[]{});

    private final long[] timestamps;
    private final double[] values;

    public DataSeriesImpl(long[] timestemps, double[] values)
    {
        this.timestamps = timestemps;
        this.values = values;
    }

    public long[] getTimestamps()
    {
        return timestamps;
    }

    public double[] getValues()
    {
        return values;
    }

    public long getFirstTimeStamp()
    {
        return timestamps==null || timestamps.length==0? 0 : timestamps[0];
    }

    public long getLastTimeStamp()
    {
        return timestamps==null || timestamps.length==0? 0 : timestamps[timestamps.length-1];
    }
}
