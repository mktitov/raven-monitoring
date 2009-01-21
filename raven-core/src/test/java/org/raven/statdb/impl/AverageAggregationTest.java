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

package org.raven.statdb.impl;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class AverageAggregationTest extends Assert
{
    @Test
    public void initWithNanTest()
    {
        AverageAggregation agg = new AverageAggregation(0, Double.NaN);
        agg.aggregate(3.);
        assertEquals(1.5, agg.getValue(), 0.);
        agg.aggregate(Double.NaN);
        assertEquals(1., agg.getValue(), 0.);
    }

    @Test
    public void initWithNumberTest()
    {
        AverageAggregation agg = new AverageAggregation(0, 1.);
        agg.aggregate(2.);
        assertEquals(1.5, agg.getValue(), 0.);
        agg.aggregate(Double.NaN);
        assertEquals(1., agg.getValue(), 0.);
    }
}