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

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.sched.impl.objects.SchedulableNode;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public class QuartzSchedulerTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        QuartzScheduler scheduler = new QuartzScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addChildren(scheduler);
        scheduler.save();
        scheduler.init();

        SchedulableNode node = new SchedulableNode();
        node.setName("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();
        node.setScheduler(scheduler);
        NodeAttribute schedule = node.getNodeAttribute(QuartzScheduler.SCHEDULE_ATTRIBUTE);
        assertNotNull(schedule);
        schedule.setValue("0/2 * * * * ?");
        schedule.save();

        node.start();
        assertEquals(Status.STARTED, node.getStatus());

        scheduler.start();
        assertEquals(Status.STARTED, scheduler.getStatus());

        TimeUnit.SECONDS.sleep(5);
        node.stop();
        assertSame(node.getExecutedBy(), scheduler);
        int counter = node.getCounter();
        assertTrue(counter>=2 && counter<=3);

        TimeUnit.SECONDS.sleep(3);
        assertEquals(counter, node.getCounter());

        node.resetCounter();
        node.start();
        TimeUnit.SECONDS.sleep(5);
        node.stop();
        counter = node.getCounter();
        assertTrue(counter>=2 && counter<=3);
        
        node.resetCounter();
        schedule.setValue("0/10 * * * * ? 2040");
        node.start();
        schedule.setValue("0/2 * * * * ?");
        TimeUnit.SECONDS.sleep(5);
        node.stop();
        assertTrue(counter>=2 && counter<=3);

        node.resetCounter();
        node.start();
        node.setScheduler(null);
//        node.getNodeAttribute("scheduler").setValue(null);
        TimeUnit.SECONDS.sleep(5);
        node.stop();
        counter = node.getCounter();
        assertTrue(counter<=1);
        
    }

    @Test
    public void scheduleAttributeTest() throws Exception
    {
         QuartzScheduler scheduler = new QuartzScheduler();
        scheduler.setName("scheduler");
        tree.getRootNode().addChildren(scheduler);
        scheduler.save();
        scheduler.init();

        SchedulableNode node = new SchedulableNode();
        node.setName("node");
        tree.getRootNode().addChildren(node);
        node.save();
        node.init();
        node.setScheduler(scheduler);
        NodeAttribute schedule = node.getNodeAttribute(QuartzScheduler.SCHEDULE_ATTRIBUTE);
        assertNotNull(schedule);
        schedule.setValue("0/2 * * * * ?");
        schedule.save();

        tree.reloadTree();
        node = (SchedulableNode) tree.getNode(node.getPath());
        assertNotNull(node);
        schedule = node.getNodeAttribute(QuartzScheduler.SCHEDULE_ATTRIBUTE);
        assertNotNull(schedule);
        
    }
}