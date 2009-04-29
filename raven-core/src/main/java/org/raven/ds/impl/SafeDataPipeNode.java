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

package org.raven.ds.impl;

import java.util.Collection;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataPipe;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SafeDataPipeNode extends AbstractDataSource implements DataPipe
{
    @NotNull @Parameter
    private DataSource dataSource;

    @Parameter
    private String expression;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useExpression;

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public Boolean getUseExpression()
    {
        return useExpression;
    }

    public void setUseExpression(Boolean useExpression)
    {
        this.useExpression = useExpression;
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setData(DataSource dataSource, Object data)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
