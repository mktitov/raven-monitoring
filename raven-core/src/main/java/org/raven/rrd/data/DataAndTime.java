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

package org.raven.rrd.data;

import java.util.Date;
import org.jrobin.core.Util;

/**
 *
 * @author Mikhail Titov
 */
public class DataAndTime
{
	private final Object data;
	private final long time;

	public DataAndTime(Object data, long time)
	{
		this.data = data;
		this.time = time;
	}

	public Object getData()
	{
		return data;
	}

	public long getTime()
	{
		return time;
	}

	@Override
	public String toString()
	{
		return new Date(time*1000).toString()+", "+data;
	}

}
