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

package org.raven.sched.impl.objects;

import org.raven.annotations.Parameter;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class SchedulableNode extends BaseNode implements Schedulable
{
    private int counter=0;

    @Parameter
    @NotNull
    private Scheduler scheduler;

    public void executeScheduledJob()
    {
        ++counter;
    }

    public int getCounter() {
        return counter;
    }

    public void resetCounter()
    {
        this.counter = 0;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

}
