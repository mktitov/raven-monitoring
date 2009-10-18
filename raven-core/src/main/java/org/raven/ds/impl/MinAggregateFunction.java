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

package org.raven.ds.impl;

import org.raven.ds.AggregateFunction;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class MinAggregateFunction implements AggregateFunction
{
    @Service
    private static TypeConverter converter;

    private Double minValue;

    public void startAggregation() { }

    public void aggregate(Object value)
    {
        Double val = converter.convert(Double.class, value, null);
        if (val!=null && (minValue==null || val<minValue))
            minValue = val;
    }

    public void finishAggregation() { }

    public Object getAggregatedValue()
    {
        return minValue;
    }
}
