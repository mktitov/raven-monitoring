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

import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.Test;
import org.raven.RavenCoreModule;
import org.raven.test.ServiceTestCase;

/**
 *
 * @author tim
 */
public class SumAggregateFunctionTest extends ServiceTestCase
{
    @Override
    protected void configureRegistry(RegistryBuilder builder)
    {
        super.configureRegistry(builder);
        builder.add(RavenCoreModule.class);
    }
    
    @Test
    public void test()
    {
        SumAggregateFunction sum = new SumAggregateFunction();
        assertEquals(0., sum.getAggregatedValue());
        sum.aggregate(1.);
        sum.aggregate("1.5");
        sum.aggregate(new Integer(1));
        assertEquals(3.5, sum.getAggregatedValue());
    }
}