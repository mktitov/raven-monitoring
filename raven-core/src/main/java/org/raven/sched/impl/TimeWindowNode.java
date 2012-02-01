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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.ValueParserException;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class)
public class TimeWindowNode extends BaseNode implements Viewable
{
    @NotNull @Parameter
    private String timePeriods;
    @Parameter
    private String daysOfWeek;
    @Parameter
    private String daysOfMonth;

    @NotNull @Parameter(defaultValue="false")
    private Boolean invertResult;
    
    @Parameter
    private TimeZone timezone;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useTimezone;

    @Message
    private static String yesMessage;
    @Message
    private static String noMessage;
    @Message
    private static String currentTimeInPeriodMessage;
    @Message
    private static String currentTimeZoneMessage;
    @Message
    private static String currentTimeInTimeZoneMessage;
    @Message
    private static String periodNameColumnMessage;
    @Message
    private static String periodStringColumnMessage;
    @Message
    private static String validPeriodColumnMessage;
    @Message
    private static String compiledPeriodColumnMessage;
    @Message
    private static String currentTimeInPeriodColumnMessage;
    @Message
    private static String timePeriodMessage;
    @Message
    private static String daysOfWeekPeriodMessage;
    @Message
    private static String daysOfMonthPeriodMessage;

    public boolean isCurrentTimeInPeriod()
    {
        try {
            boolean res = checkCurrentTimeInPeriod();
            return invertResult? !res : res;
        }
        catch (ValueParserException e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(e.getMessage(), e);
            return false;
        }
    }

    private boolean checkCurrentTimeInPeriod() throws ValueParserException
    {
        Calendar c = getCalendar();

        if (!getTimePeriod().isInPeriod(getMinutes(c)))
            return false;

        PeriodImpl dayOfWeekPeriod = getDayOfWeekPeriod();
        if (dayOfWeekPeriod != null && !dayOfWeekPeriod.isInPeriod(getDayOfWeek(c)))
            return false;

        PeriodImpl dayOfMonthPeriod = getDayOfMonthPeriod();
        if (dayOfMonthPeriod != null && !dayOfMonthPeriod.isInPeriod(getDayOfMonth(c)))
            return false;

        return true;
    }

    public String getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(String daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public String getTimePeriods() {
        return timePeriods;
    }

    public void setTimePeriods(String timePeriods) {
        this.timePeriods = timePeriods;
    }

    public Boolean getInvertResult() {
        return invertResult;
    }

    public void setInvertResult(Boolean invertResult) {
        this.invertResult = invertResult;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public void setTimezone(TimeZone timezone) {
        this.timezone = timezone;
    }

    public Boolean getUseTimezone() {
        return useTimezone;
    }

    public void setUseTimezone(Boolean useTimezone) {
        this.useTimezone = useTimezone;
    }
    
    @Parameter(readOnly=true)
    public String getTimeForDefaultTimeZone() {
        return formatDateTime(TimeZone.getDefault());
    } 
    
    @Parameter(readOnly=true)
    public String getTimeForTimeZone() {
        TimeZone tz = timezone;
        if (tz==null)
            return null;
        return formatDateTime(timezone);
    }
    
    private String formatDateTime(TimeZone timezone) {
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH);
        fmt.setTimeZone(timezone);
        return fmt.format(new Date());
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }
    
    private Calendar getCalendar() throws ValueParserException {
        TimeZone tz = null;
        if (useTimezone) {
            tz = timezone;
            if (tz==null)
                throw new ValueParserException("Timezone attribute can not be null if useTimeZone==true");
        }
        if (tz==null)
            tz = TimeZone.getDefault();

        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Selected timezone: {}", tz.getID());
        return Calendar.getInstance(tz);
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception
    {
        List<ViewableObject> vos = new ArrayList<ViewableObject>(4);
        String inv = invertResult? " (включена инверсия)" : "";
        
        Calendar c = getCalendar();
        
        vos.add(new ViewableObjectImpl(
                Viewable.RAVEN_TEXT_MIMETYPE
                , "<b>"+currentTimeInPeriodMessage+inv+" </b>"+getYesNoString(isCurrentTimeInPeriod()
                , null)));
        vos.add(new ViewableObjectImpl(
                Viewable.RAVEN_TEXT_MIMETYPE
                , "<b>"+currentTimeZoneMessage+"</b>: "+c.getTimeZone().getID()));
        vos.add(new ViewableObjectImpl(
                Viewable.RAVEN_TEXT_MIMETYPE
                , "<b>"+currentTimeInTimeZoneMessage+"</b>: "+formatDateTime(c.getTimeZone())));

        TableImpl table = new TableImpl(new String[]{
            periodNameColumnMessage, periodStringColumnMessage, validPeriodColumnMessage,
            compiledPeriodColumnMessage, currentTimeInPeriodColumnMessage});


        PeriodImpl period = null;
        boolean valid = true;
        boolean inPeriod = false;
        String error = null;
        try {
            period = getTimePeriod(); inPeriod = period.isInPeriod(getMinutes(c));
        } catch (ValueParserException e) {
            valid=false;
            error = e.getMessage();
        }
        table.addRow(new Object[]{
            timePeriodMessage,
            timePeriods,
            getYesNoString(valid, error),
            period==null? "":period.toString(),
            getYesNoString(inPeriod, null)});
        
        period = null;
        valid = true;
        inPeriod = false;
        error = null;
        try {
            period = getDayOfWeekPeriod(); 
            if (period!=null)
                inPeriod = period.isInPeriod(getDayOfWeek(c));
        } catch (ValueParserException e) {
            valid=false;
            error = e.getMessage();
        }
        table.addRow(new Object[]{
            daysOfWeekPeriodMessage,
            daysOfWeek,
            getYesNoString(valid, error),
            period==null? "":period.toString(),
            getYesNoString(inPeriod||period==null, null)});

        period = null;
        valid = true;
        inPeriod = false;
        error = null;
        try {
            period = getDayOfMonthPeriod();
            if (period!=null)
                inPeriod = period.isInPeriod(getDayOfMonth(c));
        } catch (ValueParserException e) {
            valid=false;
            error = e.getMessage();
        }
        table.addRow(new Object[]{
            daysOfMonthPeriodMessage,
            daysOfMonth,
            getYesNoString(valid, error),
            period==null? "":period.toString(),
            getYesNoString(inPeriod||period==null, null)});


        vos.add(new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table));

        return vos;
    }

    private Object getYesNoString(boolean yes, String errorMessage)
    {
        String noColor = errorMessage!=null? "red" : "blue";
        return String.format(
                    "<b><span style=\"color: %s\">%s</span></b>"
                    , yes? "green" : noColor, yes? yesMessage : noMessage)
                + (errorMessage==null? "" : " ("+errorMessage+")");
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    private PeriodImpl getTimePeriod() throws ValueParserException
    {
        return new PeriodImpl(timePeriods, new TimeValueParser());
    }

    private int getMinutes(Calendar c)
    {
        return c.get(Calendar.HOUR_OF_DAY)*60 + c.get(Calendar.MINUTE);
    }

    private int getDayOfWeek(Calendar c)
    {
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek==1? 7 : dayOfWeek-1;
    }

    private PeriodImpl getDayOfWeekPeriod() throws ValueParserException
    {
        String _daysOfWeek = daysOfWeek;
        if (_daysOfWeek!=null && !_daysOfWeek.isEmpty())
            return new PeriodImpl(_daysOfWeek, new DayOfWeekValueParser());
        else
            return null;
    }

    private PeriodImpl getDayOfMonthPeriod() throws ValueParserException
    {
        String _daysOfMonth = daysOfMonth;
        if (_daysOfMonth!=null && !_daysOfMonth.isEmpty())
            return new PeriodImpl(_daysOfMonth, new ValueParserImpl(1, 31));
        else
            return null;
    }

    private int getDayOfMonth(Calendar c)
    {
        return c.get(Calendar.DAY_OF_MONTH);
    }
}
