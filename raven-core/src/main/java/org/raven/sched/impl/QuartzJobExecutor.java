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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;

/**
 *
 * @author Mikhail Titov
 */
public class QuartzJobExecutor implements Job
{
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        Schedulable node = (Schedulable) context.getJobDetail().getJobDataMap().get(
                QuartzScheduler.NODE_ATTRIBUTE_NAME);
        Scheduler scheduler = (Scheduler) context.getJobDetail().getJobDataMap().get(
                QuartzScheduler.SCHEDULER_ATTRIBUTE_NAME);
        node.executeScheduledJob(scheduler);
    }
}
