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

package org.raven.graph.impl;

import java.util.Arrays;
import java.util.Collection;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.graph.DataSeries;
import org.raven.log.LogLevel;
import org.raven.statdb.impl.SdbQueryResultNode;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.StatisticsValues;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SdbQueryResultDataDef extends AbstractDataDef implements DataConsumer
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private DataSource dataSource;

    @Parameter @NotNull
    private String key;

    @Parameter @NotNull
    private String statisticsName;

    private ThreadLocal<QueryResult> queryResult;

    @Override
    protected void initFields()
    {
        super.initFields();

        queryResult = new ThreadLocal<QueryResult>();
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getStatisticsName()
    {
        return statisticsName;
    }

    public void setStatisticsName(String statisticsName)
    {
        this.statisticsName = statisticsName;
    }

    @Override
    public DataSeries formData(long startTime, long endTime) throws Exception
    {
        NodeAttribute startTimeAttr = new NodeAttributeImpl(
                SdbQueryResultNode.STARTTIME_SESSION_ATTRIBUTE, String.class, startTime+"L", null);
        startTimeAttr.init();

        NodeAttribute endTimeAttr = new NodeAttributeImpl(
                SdbQueryResultNode.ENDTIME_SESSION_ATTRIBUTE, String.class, endTime+"L", null);
        endTimeAttr.init();

        dataSource.getDataImmediate(this, Arrays.asList(startTimeAttr, endTimeAttr));

        QueryResult _queryResult = queryResult.get();
        if (_queryResult==null)
            return DataSeriesImpl.EMPTY_DATA_SERIES;
        else
        {
            String _key = key;
            String _statisticsName = statisticsName;
            for (KeyValues keyValues: _queryResult.getKeyValues())
                if (keyValues.getKey().equals(_key))
                {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format("Key (%s) found in the query result", _key));
                    for (StatisticsValues statisticsValues: keyValues.getStatisticsValues())
                        if (statisticsValues.getStatisticsName().equals(_statisticsName))
                        {
                            if (isLogLevelEnabled(LogLevel.DEBUG))
                                debug(String.format(
                                        "Found data for statistics name (%s)", _statisticsName));
                            return new DataSeriesImpl(
                                    _queryResult.getTimestamps(), statisticsValues.getValues());
                        }
                }

            if (isLogLevelEnabled(LogLevel.WARN))
                warn(String.format(
                        "No data found in query result for key (%s) and statistics name (%s)"
                        , _key, _statisticsName));

            return DataSeriesImpl.EMPTY_DATA_SERIES;
        }

    }

    public void setData(DataSource dataSource, Object data)
    {
        if (data instanceof QueryResult)
        {
            queryResult.set((QueryResult)data);
        }
        else if (isLogLevelEnabled(LogLevel.WARN))
        {
            debug(String.format(
                    "Invalid data type recieved from (%s). Expected (%s) but recieved (%s)"
                    , dataSource.getPath(), QueryResult.class.getName()
                    , (data==null? "NULL" : data.getClass().getName())));
        }
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
