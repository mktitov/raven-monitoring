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
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class AttributeValueDataSourceNode extends BaseNode implements DataSource
{
    @Parameter
    private String value;

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        if (!getStatus().equals(Status.STARTED))
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s). Data source not started"
                        , dataConsumer.getPath()));
            return false;
        }
        try
        {
            dataConsumer.setData(this, value);
            return true;
        }
        catch(Throwable e)
        {
            if (isLogLevelEnabled(LogLevel.ERROR))
                error(String.format(
                        "Error gathering data for consumer (%s)", dataConsumer.getPath()));
            return false;
        }
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }
}
