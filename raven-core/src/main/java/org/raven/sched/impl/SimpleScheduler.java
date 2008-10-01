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

package org.raven.sched.impl;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.sched.Scheduler;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SchedulersNode.class)
public class SimpleScheduler extends BaseNode implements Scheduler
{
    @Parameter(defaultValue="MINUTES")
    @NotNull
    private TimeUnit intervalUnit;
    
    @Parameter(defaultValue="5")
    @NotNull
    private Integer interval;

    @Parameter(defaultValue="3")
    private Integer corePoolSize;
    
    @Parameter(defaultValue="6")
    private Integer maximumPoolSize;
    
    public Integer getInterval()
    {
        return interval;
    }

    public void setInterval(Integer interval)
    {
        this.interval = interval;
    }

    public TimeUnit getIntervalUnit()
    {
        return intervalUnit;
    }

    public void setIntervalUnit(TimeUnit intervalUnit)
    {
        this.intervalUnit = intervalUnit;
    }

    public Integer getCorePoolSize()
    {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize)
    {
        this.corePoolSize = corePoolSize;
    }

    public Integer getMaximumPoolSize()
    {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize)
    {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

}
