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

package org.raven.sched.impl;

import java.util.Calendar;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;

/**
 *
 * @author Mikhail Titov
 */
public class TimeWindowHelperTest extends RavenCoreTestCase
{
    @Test
    public void test()
    {
        Calendar c = Calendar.getInstance();

        TimeWindowNode node1 = new TimeWindowNode();
        node1.setName("timeWindowNode1");
        tree.getRootNode().addAndSaveChildren(node1);
        node1.setTimePeriods(""+c.get(Calendar.HOUR_OF_DAY)+","+(c.get(Calendar.HOUR_OF_DAY)+1));

        assertFalse(TimeWindowHelper.isCurrentDateInPeriod(tree.getRootNode()));
        assertTrue(node1.start());
        assertTrue(TimeWindowHelper.isCurrentDateInPeriod(tree.getRootNode()));
        node1.setTimePeriods(""+(c.get(Calendar.HOUR_OF_DAY)-1));
        assertFalse(TimeWindowHelper.isCurrentDateInPeriod(tree.getRootNode()));
    }
}