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
import org.raven.statdb.query.QueryResult;
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
        QueryResult queryResult = statisticsDatabase.executeQuery(queryNode);
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
}
