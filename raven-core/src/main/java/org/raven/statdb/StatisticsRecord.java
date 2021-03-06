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

import java.util.Map;

/**
 *
 * @author Mikhail Titov
 */
public interface StatisticsRecord
{
    public final static String KEY_FIELD_NAME = "key";
    public final static String TIME_FIELD_NAME = "time";
	/**
	 * Returns the statics key. The key may be composite. The delmiters of the key parts is
	 * <b color="BLUE">/<b>
	 */
	public String getKey();
	/**
	 * Returns the values of statistics. The key in the map is statistics name the value is a
	 * statistics value. The method can return the null value.
	 */
	public Map<String, Double> getValues();
	/**
	 * Returns the statics time in seconds.
	 */
	public long getTime();

    public String[] getKeyElements();
}
