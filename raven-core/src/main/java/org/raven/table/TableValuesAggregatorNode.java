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

package org.raven.table;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.ValuesAggregatorNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=TableSummaryNode.class)
public class TableValuesAggregatorNode extends ValuesAggregatorNode
{
    public final static String SELECTOR_ATTR = "selector";
    public final static String GROUP_EXPRESSION_ATTR = "groupExpression";
    public final static String TITLE_ATTR = "title";

    @Parameter(defaultValue="true")
    private Boolean selector;

    @Parameter
    private String title;

    @Parameter(defaultValue="COLUMN")
    private AggregationDirection aggregationDirection;

    @Parameter
    private Object groupExpression;

    public Object getGroupExpression()
    {
        return groupExpression;
    }

    public void setGroupExpression(Object groupExpression)
    {
        this.groupExpression = groupExpression;
    }

    public AggregationDirection getAggregationDirection()
    {
        return aggregationDirection;
    }

    public void setAggregationDirection(AggregationDirection aggregationDirection)
    {
        this.aggregationDirection = aggregationDirection;
    }

    public Boolean getSelector()
    {
        return selector;
    }

    public void setSelector(Boolean selector)
    {
        this.selector = selector;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
