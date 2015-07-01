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
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.ds.SessionAttributeGenerator;
import org.raven.tree.NodeAttribute;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SafeDataPipeNode.class)
public class DataSourceSessionAttributeNode
        extends DataSourceFieldValueGenerator implements SessionAttributeGenerator
{
    @NotNull @Parameter
    private Class attributeType;

    @NotNull @Parameter(defaultValue="false")
    private Boolean forwardDataSourceAttributes;

    public Class getAttributeType()
    {
        return attributeType;
    }

    public void setAttributeType(Class attributeType)
    {
        this.attributeType = attributeType;
    }

    public Boolean getForwardDataSourceAttributes()
    {
        return forwardDataSourceAttributes;
    }

    public void setForwardDataSourceAttributes(Boolean forwardDataSourceAttributes)
    {
        this.forwardDataSourceAttributes = forwardDataSourceAttributes;
    }

    public void fillConsumerAttributes(Collection<NodeAttribute> attributes)
    {
        Boolean _forwardDataSourceAttributes = forwardDataSourceAttributes;
        DataSource _dataSource = getDataSource();
        if (_forwardDataSourceAttributes!=null && _forwardDataSourceAttributes && _dataSource!=null)
        {
            Collection<NodeAttribute> dsAttrs = _dataSource.generateAttributes();
            if (dsAttrs!=null && !dsAttrs.isEmpty())
                attributes.addAll(dsAttrs);
        }
    }

    @Override
    protected boolean allowAttributesGeneration(NodeAttribute attr)
    {
        if (   attr.getName().equals(DATASOURCE_ATTR)
            && forwardDataSourceAttributes!=null
            && forwardDataSourceAttributes)
        {
            return false;
        }
        else
            return super.allowAttributesGeneration(attr);
    }
}
