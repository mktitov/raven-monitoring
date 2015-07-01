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
import java.util.List;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class TimeWindowNodeTest extends RavenCoreTestCase
{
    private TimeWindowNode timeWindow;
    private Calendar c;

    @Before
    public void prepare()
    {
        c = Calendar.getInstance();
        timeWindow = new TimeWindowNode();
        timeWindow.setName("time window");
        tree.getRootNode().addAndSaveChildren(timeWindow);
        timeWindow.setTimePeriods(""+c.get(Calendar.HOUR_OF_DAY)+","+(c.get(Calendar.HOUR_OF_DAY)+1));
        assertTrue(timeWindow.start());
    }

    @Test
    public void timePeriodsTest()
    {
        assertTrue(timeWindow.isCurrentTimeInPeriod());
        timeWindow.setTimePeriods(""+(c.get(Calendar.HOUR_OF_DAY)-1));
        assertFalse(timeWindow.isCurrentTimeInPeriod());
    }

    @Test
    public void daysOfWeekTest()
    {
        int day = c.get(Calendar.DAY_OF_WEEK);
        day = day==1? 7 : day-1;
        timeWindow.setDaysOfWeek(""+day);
        assertTrue(timeWindow.isCurrentTimeInPeriod());
        timeWindow.setDaysOfWeek(""+(day==1? day+1 : day-1));
        assertFalse(timeWindow.isCurrentTimeInPeriod());
    }

    @Test
    public void daysOfMonthTest()
    {
        int day = c.get(Calendar.DAY_OF_MONTH);
        timeWindow.setDaysOfMonth(""+day);
        assertTrue(timeWindow.isCurrentTimeInPeriod());
        timeWindow.setDaysOfMonth(""+(day==1? day+1 : day-1));
        assertFalse(timeWindow.isCurrentTimeInPeriod());
    }

    @Test
    public void invertResultTest()
    {
        timeWindow.setInvertResult(Boolean.TRUE);
        assertFalse(timeWindow.isCurrentTimeInPeriod());
        timeWindow.setTimePeriods(""+(c.get(Calendar.HOUR_OF_DAY)-1));
        assertTrue(timeWindow.isCurrentTimeInPeriod());
    }

    @Test
    public void getViewableObjectsTest() throws Exception
    {
        List<ViewableObject> vos = timeWindow.getViewableObjects(null);
        assertNotNull(vos);
        assertEquals(4, vos.size());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(0).getMimeType());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(1).getMimeType());
        assertEquals(Viewable.RAVEN_TEXT_MIMETYPE, vos.get(2).getMimeType());
        assertEquals(Viewable.RAVEN_TABLE_MIMETYPE, vos.get(3).getMimeType());
    }
    
    @Test
    public void timezoneTest() {
        TimeZone timezone = TimeZone.getDefault();
        long offset = timezone.getRawOffset()/(1000*60*60);
        timeWindow.setUseTimezone(Boolean.TRUE);
        timeWindow.setTimezone(TimeZone.getTimeZone("GMT+"+(offset-2)));
        assertFalse(timeWindow.isCurrentTimeInPeriod());      
        
        timeWindow.setTimezone(TimeZone.getDefault());
        assertTrue(timeWindow.isCurrentTimeInPeriod());      
    }
}