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

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SchedulableDataPipe extends DataPipeImpl implements Schedulable, Scheduler
{
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull
    private Scheduler scheduler;

    public void executeScheduledJob(Scheduler scheduler)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug("Initiating data gathering request");
        getDataSource().getDataImmediate(this, null);

		Collection<Node> deps = getDependentNodes();
		if (deps!=null)
			for (Node node: deps)
				if (node instanceof Schedulable)
					((Schedulable)node).executeScheduledJob(this);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

}
