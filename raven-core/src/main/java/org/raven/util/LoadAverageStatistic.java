/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author Mikhail Titov
 */
public class LoadAverageStatistic
{
    private final long loadAverageInterval;
    private final int elementCount;

    private long duration = 0;
    private long timePeriod = 0;
    private double loadAverage = 0;

    /**
     * 
     * @param loadAverageInterval interval in milliseconds
     * @param elementCount the count of elements that works in parellel (for instance,
     *      the number of threads in the pool or a number of phone terminals in the pool.
     */
    public LoadAverageStatistic(long loadAverageInterval, int elementCount)
    {
        this.loadAverageInterval = loadAverageInterval;
        this.elementCount = elementCount;
    }

    /**
     * Adds one element usage duration in milliseconds
     * @param dur duration in milliseconds
     */
    public void addDuration(long dur)
    {
        long currTimePeriod = System.currentTimeMillis()/loadAverageInterval;
        if (currTimePeriod>timePeriod){
            timePeriod = currTimePeriod;
            if (timePeriod>0){
                loadAverage = duration*100./(elementCount*loadAverageInterval);
                loadAverage = new BigDecimal(loadAverage).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
            duration = dur;
        }else
            duration += dur;
    }

    /**
     * Returns the load average for the last interval.
     */
    public double getValue() {
        return loadAverage;
    }

    @Override
    public String toString()
    {
        return String.format("%.2f%%", loadAverage);
    }
}
