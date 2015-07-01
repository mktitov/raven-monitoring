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

package org.raven.statdb;

import org.raven.statdb.impl.AverageAggregation;
import org.raven.statdb.impl.LastAggregation;
import org.raven.statdb.impl.MaxAggregation;
import org.raven.statdb.impl.MinAggregation;
import org.raven.statdb.impl.SumAggregation;

/**
 *
 * @author Mikhail Titov
 */
public enum AggregationFunction
{
	MIN, MAX, AVERAGE, LAST, SUM;

    public Aggregation createAggregation(long time, double initialValue)
    {
        switch (this)
        {
            case AVERAGE : return new AverageAggregation(time, initialValue);
            case LAST    : return new LastAggregation(time, initialValue);
            case MAX     : return new MaxAggregation(time, initialValue);
            case MIN     : return new MinAggregation(time, initialValue);
            case SUM     : return new SumAggregation(time, initialValue);
        }

        throw new UnsupportedOperationException(
                "createFunction does not support the "+this.toString()+" aggregation");
    }
}
