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
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.statdb.query.SelectClause;
import org.raven.statdb.query.SelectMode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SdbQueryNode  extends BaseNode implements Query
{
    @Parameter() @NotNull
    private Long step;

    @Parameter() @NotNull()
    private String startTime;

    @Parameter() @NotNull()
    private String endTime;

    @Parameter() 
    private Integer maximumKeyCount;

    @Parameter(defaultValue="SELECT_KEYS_AND_DATA")
    @NotNull
    private SelectMode selectMode;

    private StatisticsNamesNode statisticsNamesNode;
    private FromClauseNode fromClauseNode;
    private SelectClauseNode selectClauseNode;

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

    public FromClauseNode getFromClauseNode()
    {
        return fromClauseNode;
    }

    public void setFromClauseNode(FromClauseNode fromClauseNode)
    {
        this.fromClauseNode = fromClauseNode;
    }

    public SelectClauseNode getSelectClauseNode()
    {
        return selectClauseNode;
    }

    public void setSelectClauseNode(SelectClauseNode selectClauseNode)
    {
        this.selectClauseNode = selectClauseNode;
    }

    public Long getStep()
    {
        return step;
    }

    public void setStep(Long step)
    {
        this.step = step;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }

    public Integer getMaximumKeyCount()
    {
        return maximumKeyCount;
    }

    public void setMaximumKeyCount(Integer maximumKeyCount)
    {
        this.maximumKeyCount = maximumKeyCount;
    }

    public StatisticsNamesNode getStatisticsNamesNode()
    {
        return statisticsNamesNode;
    }

    public QueryStatisticsName[] getStatisticsNames()
    {
        Collection<Node> statNames = statisticsNamesNode.getSortedChildrens();
        if (statNames==null || statNames.size()==0)
            return null;

        QueryStatisticsName[] result = new QueryStatisticsName[statNames.size()];
        int i=0;
        for (Node statName: statNames)
            if (statName.getStatus().equals(Status.STARTED))
                result[i++] = (QueryStatisticsName) statName;

        if (i==0)
            return null;

        if (i<result.length)
        {
            QueryStatisticsName[] newResult = new QueryStatisticsName[i];
            System.arraycopy(result, 0, newResult, 0, i);
            result = newResult;
        }

        return result;
    }

    public FromClause getFromClause()
    {
        return fromClauseNode;
    }

    public SelectClause getSelectClause()
    {
        return selectClauseNode;
    }

    public SelectMode getSelectMode()
    {
        return selectMode;
    }

    public void setSelectMode(SelectMode selectMode)
    {
        this.selectMode = selectMode;
    }

    private void generateNodes()
    {
        statisticsNamesNode = (StatisticsNamesNode) getChildren(StatisticsNamesNode.NAME);
        if (statisticsNamesNode == null)
        {
            statisticsNamesNode = new StatisticsNamesNode();
            statisticsNamesNode.setParent(this);
            statisticsNamesNode.save();
            addChildren(statisticsNamesNode);
            statisticsNamesNode.init();
            statisticsNamesNode.start();
        }
        fromClauseNode = (FromClauseNode) getChildren(FromClauseNode.NAME);
        if (fromClauseNode == null)
        {
            fromClauseNode = new FromClauseNode();
            this.addAndSaveChildren(fromClauseNode);
        }
        selectClauseNode = (SelectClauseNode) getChildren(SelectClauseNode.NAME);
        if (selectClauseNode == null)
        {
            selectClauseNode = new SelectClauseNode();
            addAndSaveChildren(selectClauseNode);
            selectClauseNode.start();
        }
    }
}
