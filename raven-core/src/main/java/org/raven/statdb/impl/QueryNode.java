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
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class QueryNode  extends BaseNode implements Query
{
    @Parameter() @NotNull
    private Long step;

    @Parameter() @NotNull()
    private String startTime;

    @Parameter() @NotNull()
    private String endTime;

    @Parameter() 
    private Integer maximumKeyCount;

    private StatisticsNamesNode statisticsNames;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        statisticsNames = (StatisticsNamesNode) getChildren(StatisticsNamesNode.NAME);
        if (statisticsNames==null)
        {
            statisticsNames = new StatisticsNamesNode();
            statisticsNames.setParent(this);
            statisticsNames.save();
            addChildren(statisticsNames);
            statisticsNames.init();
            statisticsNames.start();
        }
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
        return statisticsNames;
    }

    public QueryStatisticsName[] getStatisticsNames()
    {
        Collection<Node> statNames = statisticsNames.getSortedChildrens();
        if (statNames==null || statNames.size()==0)
            return null;

        QueryStatisticsName[] result = new QueryStatisticsName[statNames.size()];
        int i=0;
        for (Node statName: statNames)
            result[i++] = (QueryStatisticsName) statName;

        return result;
    }

    public FromClause getFromClause()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SelectClause getSelectClause()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
