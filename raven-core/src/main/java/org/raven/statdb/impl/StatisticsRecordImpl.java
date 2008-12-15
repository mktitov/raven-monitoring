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

package org.raven.statdb.impl;

import java.util.HashMap;
import java.util.Map;
import org.raven.statdb.StatisticsRecord;

/**
 *
 * @author Mikhail Titov
 */
public class StatisticsRecordImpl implements StatisticsRecord
{
	private final String key;
	private final long time;
	private Map<String, Double> values;

	public StatisticsRecordImpl(String key, long time)
	{
		this.key = key;
		this.time = time;
	}

	public StatisticsRecordImpl(String key)
	{
		this(key, System.currentTimeMillis());
	}

	public void put(String statsticsName, Double value)
	{
		if (values==null)
			values = new HashMap<String, Double>();
		values.put(statsticsName, value);
	}

	public String getKey()
	{
		return key;
	}

	public long getTime()
	{
		return time;
	}

	public Map<String, Double> getValues()
	{
		return values;
	}
}
