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

package org.raven.tree.impl;

import org.junit.Test;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.ServiceStateAlarm;
/**
 *
 * @author Mikhail Titov
 */
public class ServiceAvailabilityControlNodeTest extends RavenCoreTestCase
{
    @Test
    public void test() throws Exception
    {
        PushDataSource ds = new PushDataSource();
        ds.setName("data source");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());

        ServiceStateNode serviceState = new ServiceStateNode();
        serviceState.setName("service state");
        tree.getRootNode().addAndSaveChildren(serviceState);
        assertTrue(serviceState.start());

        ServiceAvailabilityControlNode aNode = new ServiceAvailabilityControlNode();
        aNode.setName("availability control");
        serviceState.addAndSaveChildren(aNode);
        aNode.getNodeAttribute(ServiceStateControlNode.STATE_EXPRESSION_ATTRIBUTE).setValue("data");
        aNode.setDataSource(ds);
        assertTrue(aNode.start());

        assertNull(serviceState.getServiceAvailability());
        assertNull(serviceState.getServiceAvailabilityAlarm());

        ds.pushData(new Byte((byte)100));
        assertEquals(new Byte((byte)100), serviceState.getServiceAvailability());
        assertEquals(ServiceStateAlarm.NO_ALARM, serviceState.getServiceAvailabilityAlarm());

        ds.pushData(new Byte((byte)50));
        assertEquals(new Byte((byte)50), serviceState.getServiceAvailability());
        assertEquals(ServiceStateAlarm.WARNING, serviceState.getServiceAvailabilityAlarm());

        ds.pushData(new Byte((byte)0));
        assertEquals(new Byte((byte)0), serviceState.getServiceAvailability());
        assertEquals(ServiceStateAlarm.CRITICAL, serviceState.getServiceAvailabilityAlarm());
    }
}