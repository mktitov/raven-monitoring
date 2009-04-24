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

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.raven.Helper;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=SchedulersNode.class)
public class QuartzScheduler extends BaseNode implements Scheduler
{
    public final static String NODE_ATTRIBUTE_NAME = "NODE";
    public final static String SCHEDULE_ATTRIBUTE = "schedule";

    @Parameter(defaultValue="20")
    @NotNull
    private Integer threadCount;

    @Message
    private String scheduleDescription;

    private org.quartz.Scheduler scheduler;

    //readonly attributes
    private AtomicLong numJobsExecuted;
    private AtomicLong numJobExecutionErrors;
    private AtomicLong numJobsExecuting;
    private AtomicLong avgJobExecutionTime;
    private AtomicLong numMisfires;

    @Parameter(readOnly=true)
    public AtomicLong getAvgJobExecutionTime()
    {
        return avgJobExecutionTime;
    }

    @Parameter(readOnly=true)
    public AtomicLong getNumJobExecutionErrors()
    {
        return numJobExecutionErrors;
    }

    @Parameter(readOnly=true)
    public AtomicLong getNumJobsExecuted()
    {
        return numJobsExecuted;
    }

    @Parameter(readOnly=true)
    public AtomicLong getNumJobsExecuting()
    {
        return numJobsExecuting;
    }

    @Parameter(readOnly=true)
    public AtomicLong getNumMisfires()
    {
        return numMisfires;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        resetCounters();
    }

    private void resetCounters()
    {
        numJobsExecuted = new AtomicLong();
        numJobExecutionErrors = new AtomicLong();
        numJobsExecuting = new AtomicLong();
        avgJobExecutionTime = new AtomicLong();
        numMisfires = new AtomicLong();
    }

    public Integer getThreadCount()
    {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount)
    {
        this.threadCount = threadCount;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        resetCounters();
        scheduler = null;
        int _threadCount = threadCount;
        if (_threadCount<1)
            throw new Exception("The value of the attribute threadCount must be greater than 1");
        
        Properties props = new Properties();
        props.put("org.quartz.threadPool.class", SimpleThreadPool.class.getName());
        props.put("org.quartz.threadPool.threadCount", ""+_threadCount);
        StdSchedulerFactory factory = new StdSchedulerFactory(props);
        factory.initialize();
        scheduler = factory.getScheduler();
        SchedulerJobListener listener = new SchedulerJobListener();
        scheduler.addGlobalJobListener(listener);
        scheduler.addGlobalTriggerListener(listener);
        scheduler.start();

        Collection<Node> deps = getDependentNodes();
        if (deps!=null)
            for (Node dep: deps)
                if (dep instanceof Schedulable && dep.getStatus()==Status.STARTED)
                    addJob((Schedulable)dep);
    }

    @Override
    public synchronized void stop() throws NodeError
    {
        try
        {
            if (scheduler!=null)
                scheduler.shutdown(false);
        }
        catch (SchedulerException ex)
        {
            throw new NodeError(String.format("Error stoping scheduler (%s)", getPath()), ex);
        }

        super.stop();
    }

    @Override
    public boolean addDependentNode(Node dependentNode)
    {
        boolean result =  super.addDependentNode(dependentNode);

        if (result && dependentNode instanceof Schedulable)
            dependentNode.addListener(this);

        return result;
    }

    @Override
    public boolean removeDependentNode(Node dependentNode)
    {
        boolean result = super.removeDependentNode(dependentNode);
        
        if (result && dependentNode instanceof Schedulable)
        {
            dependentNode.removeListener(this);
            removeJob((Schedulable)dependentNode);
        }

        return result;
    }

    @Override
    public synchronized void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
        if (getStatus()==Status.STARTED && node!=this && node instanceof Schedulable)
        {
            synchronized(this)
            {
                if (newStatus==Status.STARTED)
                    addJob((Schedulable)node);
                else
                    removeJob((Schedulable)node);
            }
        }
    }

    @Override
    public synchronized void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        if (getStatus()==Status.STARTED 
            && node!=this
            && node instanceof Schedulable
            && node.getStatus()==Status.STARTED
            && attribute.getName().equals(SCHEDULE_ATTRIBUTE))
        {
            removeJob((Schedulable)node);
            addJob((Schedulable) node);
        }
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        NodeAttribute attr = new NodeAttributeImpl(
                SCHEDULE_ATTRIBUTE, String.class, null, scheduleDescription);
        attr.setRequired(true);

        return Arrays.asList(attr);
    }

    private void addJob(Schedulable node)
    {
        if (!Helper.checkAttributes(this, generateAttributes(), node))
            return ;
        String schedule = node.getNodeAttribute(SCHEDULE_ATTRIBUTE).getRealValue();
        try
        {
            CronTrigger trigger = new CronTrigger(
                    "" + node.getId(), scheduler.DEFAULT_GROUP, schedule);
            JobDetail jobDetail = new JobDetail(
                    ""+node.getId(), scheduler.DEFAULT_GROUP, QuartzJobExecutor.class);
            jobDetail.getJobDataMap().put(NODE_ATTRIBUTE_NAME, node);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (ParseException ex)
        {
            node.getLogger().error(String.format(
                    "Node (%s) error. Invalid cron expression for attribute (schedule). %s"
                    , node.getPath(), ex.getMessage()));
            node.stop();
        }
        catch (SchedulerException se)
        {
            node.getLogger().error(String.format(
                    "Error adding node (%s) to the quartz scheduler (%s). %s"
                    , node.getPath(), getPath(), se.getMessage()));
        }
    }

    private void removeJob(Schedulable node)
    {
        try
        {
            org.quartz.Scheduler _scheduler = scheduler;
            if (_scheduler!=null)
                _scheduler.deleteJob("" + node.getId(), _scheduler.DEFAULT_GROUP);
        }
        catch (SchedulerException ex)
        {
            logger.error(String.format(
                    "Error removing (%s) job from quartz scheduler (%s). %s"
                    , node.getPath(), getPath(), ex.getMessage()));
        }
    }

    private class SchedulerJobListener implements JobListener, TriggerListener
    {
        public String getName()
        {
            return "GloabalListener";
        }

        public void jobToBeExecuted(JobExecutionContext context)
        {
            numJobsExecuting.incrementAndGet();
        }

        public void jobExecutionVetoed(JobExecutionContext context) {
        }

        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException)
        {
            numJobsExecuting.decrementAndGet();
            long executedJobs = numJobsExecuted.incrementAndGet();
            if (jobException!=null)
                numJobExecutionErrors.incrementAndGet();
            if (executedJobs==1)
                avgJobExecutionTime.set(context.getJobRunTime());
            else
                avgJobExecutionTime.set((avgJobExecutionTime.get()+context.getJobRunTime())/2);
        }

        public void triggerFired(Trigger trigger, JobExecutionContext context) {
        }

        public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context)
        {
            return false;
        }

        public void triggerMisfired(Trigger trigger)
        {
            numMisfires.incrementAndGet();
        }

        public void triggerComplete(
                Trigger trigger, JobExecutionContext context, int triggerInstructionCode) 
        {
        }
        
    }
}
