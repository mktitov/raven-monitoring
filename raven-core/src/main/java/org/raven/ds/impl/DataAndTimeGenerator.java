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

package org.raven.ds.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.rrd.data.DataAndTime;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class DataAndTimeGenerator extends DataPipeImpl
{
	@Parameter(defaultValue="dd.MM.yyyy HH:mm:ss")
	@NotNull
	private String datePattern;

	@Parameter
	@NotNull
	private String date;

	@Parameter
	private Locale locale;

	public String getDate()
	{
		return date;
	}

	public void setDate(String date)
	{
		this.date = date;
	}

	public String getDatePattern()
	{
		return datePattern;
	}

	public void setDatePattern(String datePattern)
	{
		this.datePattern = datePattern;
	}

	public Locale getLocale()
	{
		return locale;
	}

	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}

	@Override
	protected void sendDataToConsumer(DataConsumer consumer, Object data)
	{
		try
		{
			Locale _locale = locale;
			if (_locale==null)
				_locale = Locale.getDefault();
			SimpleDateFormat formatter = new SimpleDateFormat(datePattern, _locale);
			Date realDate = formatter.parse(date);
			DataAndTime dataAndTime = new DataAndTime(data, realDate.getTime()/1000);

			super.sendDataToConsumer(consumer, dataAndTime);
		}
		catch (ParseException ex)
		{
			error(String.format(
					"Error converting (%s) to date using (%s) pattern", date, datePattern), ex);
		}
	}

	
}
