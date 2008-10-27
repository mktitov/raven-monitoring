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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jrobin.core.Util;
import org.raven.tree.Node;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DataSample
{
	@Service
	private static TypeConverter converter;

	private final RRDNode rrdNode;
	private long sampleTime = 0l;
	private Map<RRDataSource, Double> values;

	public DataSample(RRDNode rrdNode)
	{
		this.rrdNode = rrdNode;
	}

	public Map<RRDataSource, Double> getValues()
	{
		return values==null? Collections.EMPTY_MAP : values;
	}

	public long getSampleTime()
	{
		return sampleTime;
	}

	public boolean isAllValuesSetted()
	{
		if (values==null)
			return false;

		Collection<Node> deps = rrdNode.getDependentNodes();
		if (deps!=null)
			for (Node dep: deps)
				if (   dep instanceof RRDataSource
					&& dep.getStatus()==Node.Status.STARTED
					&& !values.containsKey(dep))
				{
					return false;
				}
		else
			return false;

		return true;
	}

	public boolean checkAndSetValue(RRDataSource ds, Object value)
	{
		Object objVal = null;
		long time = 0;
		if (value instanceof DataAndTime)
		{
			DataAndTime data = (DataAndTime) value;
			time = data.getTime();
			objVal = data.getData();
		}
		else
		{
			time = Util.getTime();
			objVal = value;
		}

		time = Util.normalize(time, rrdNode.getStep());
		if (sampleTime==0)
			sampleTime = time;
		else
		{
			if (time!=sampleTime)
				return false;
		}

		Double doubleVal = converter.convert(Double.class, objVal, null);
		if (values==null)
			values = new HashMap<RRDataSource, Double>();
		values.put(ds, doubleVal);

		return true;
	}
}
