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

package org.raven.ds.impl;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 * Data consumer that collects data from one data source.
 * 
 * @author Mikhail Titov
 */
public abstract class AbstractDataConsumer extends ContainerNode implements DataConsumer
{
    public final static String DATASOURCE_ATTRIBUTE = "dataSource";

    public enum ResetDataPolicy {
        DONT_RESET_DATA, RESET_LAST_DATA, RESET_PREVIOUS_DATA, RESET_LAST_AND_PREVIOUS_DATA};
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull 
    @Description("The data source")
    private DataSource dataSource;

    @Parameter(defaultValue="RESET_LAST_AND_PREVIOUS_DATA")
//    @Parameter()
    @NotNull
    private ResetDataPolicy resetDataPolicy;

    protected Object data;
    private long lastDataTime;
    private Object previousData;
    private long previousDataTime;

    public ResetDataPolicy getResetDataPolicy() {
        return resetDataPolicy;
    }

    public void setResetDataPolicy(ResetDataPolicy resetDataPolicy) {
        this.resetDataPolicy = resetDataPolicy;
    }

    @Parameter(readOnly=true)
    public Object getLastData()
    {
        return data;
    }

    @Parameter(readOnly=true)
    public long getLastDataTimeMillis()
    {
        return lastDataTime;
    }

    @Parameter(readOnly=true)
    public String getLastDataTime()
    {
        return lastDataTime==0?
            "" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(lastDataTime));
    }

    @Parameter(readOnly=true)
    public Object getPreviousData()
    {
        return previousData;
    }

    @Parameter(readOnly=true)
    public long getPreviuosDataTimeMillis()
    {
        return previousDataTime;
    }

    @Parameter(readOnly=true)
    public String getPreviousDataTime()
    {
        return previousDataTime==0?
            "" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(previousDataTime));
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setData(DataSource dataSource, Object data) 
    {
        if (Status.STARTED!=getStatus())
        {
            logger.error(String.format(
                    "Error pushing data to the node (%s) from the (%s) node. Node NOT STARTED"
                    , getPath(), dataSource.getPath()));
            return;
        }
        this.previousData = this.data;
        this.previousDataTime = this.lastDataTime;
        this.data = data;
        this.lastDataTime = System.currentTimeMillis();
        try{
            doSetData(dataSource, data);
        }finally
        {
//            previousData = data;
//            previousDataTime = lastDataTime;

            switch(resetDataPolicy)
            {
                case RESET_LAST_AND_PREVIOUS_DATA: this.data = null; previousData = null; break;
                case RESET_LAST_DATA: this.data = null; break;
                case RESET_PREVIOUS_DATA: this.previousData = null; break;
            }
        }

    }
    
    protected abstract void doSetData(DataSource dataSource, Object data);

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) 
    {
        if (Status.STARTED==getStatus())
        {
            dataSource.getDataImmediate(this, sessionAttributes);
            return data;
        }
        else
        {
            getLogger().error(String.format(
                    "Error refreshing data in the node (%s). Node NOT STARTED", getPath()));
            return null;
        }
    }
    
    
}
