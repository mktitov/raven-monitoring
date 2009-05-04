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

package org.raven.statdb.impl;

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.statdb.query.SelectClause;
import org.raven.statdb.query.SelectMode;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SdbQueryResultNode extends AbstractDataSource
{
    public final static String QUERY_NODE_NAME = "query";
    public final static String STARTTIME_SESSION_ATTRIBUTE = "startTime";
    public final static String ENDTIME_SESSION_ATTRIBUTE = "endTime";

    private SdbQueryNode queryNode;

    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private StatisticsDatabase statisticsDatabase;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        generateNodes();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        generateNodes();
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes)
        throws Exception
    {
        QueryWrapper query = new QueryWrapper(
                queryNode
                , getAttributeValue(STARTTIME_SESSION_ATTRIBUTE, attributes)
                , getAttributeValue(ENDTIME_SESSION_ATTRIBUTE, attributes));
        QueryResult queryResult = statisticsDatabase.executeQuery(query);
        dataConsumer.setData(this, queryResult);
        
        return true;
    }

    public StatisticsDatabase getStatisticsDatabase()
    {
        return statisticsDatabase;
    }

    public void setStatisticsDatabase(StatisticsDatabase statisticsDatabase)
    {
        this.statisticsDatabase = statisticsDatabase;
    }

    public SdbQueryNode getQueryNode()
    {
        return queryNode;
    }

    public void setQueryNode(SdbQueryNode queryNode)
    {
        this.queryNode = queryNode;
    }

    private void generateNodes()
    {
        queryNode = (SdbQueryNode) getChildren(QUERY_NODE_NAME);
        if (queryNode==null)
        {
            queryNode = new SdbQueryNode();
            queryNode.setName(QUERY_NODE_NAME);
            this.addAndSaveChildren(queryNode);
            queryNode.start();
        }
    }

    private String getAttributeValue(String attributeName, Map<String, NodeAttribute> attrs)
    {
        NodeAttribute attr = attrs.get(attributeName);
        return attr==null? null : attr.getValue();
    }

    private class QueryWrapper implements Query
    {
        private final Query query;
        private final String newStartTime;
        private final String newEndTime;

        public QueryWrapper(Query query, String newStartTime, String newEndTime)
        {
            this.query = query;
            this.newStartTime = newStartTime;
            this.newEndTime = newEndTime;
        }

        public Long getStep()
        {
            return query.getStep();
        }

        public SelectMode getSelectMode()
        {
            return query.getSelectMode();
        }

        public String getStartTime()
        {
            return newStartTime==null? query.getStartTime() : newStartTime;
        }

        public String getEndTime()
        {
            return newEndTime==null? query.getEndTime() : newEndTime;
        }

        public Integer getMaximumKeyCount()
        {
            return query.getMaximumKeyCount();
        }

        public QueryStatisticsName[] getStatisticsNames()
        {
            return query.getStatisticsNames();
        }

        public FromClause getFromClause()
        {
            return query.getFromClause();
        }

        public SelectClause getSelectClause()
        {
            return query.getSelectClause();
        }
    }
}
