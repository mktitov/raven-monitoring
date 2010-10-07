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

import org.junit.Before;
import org.junit.Test;
import org.raven.test.PushDataSource;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.ServiceStateAlarm;
import static org.raven.tree.ServiceStateAlarm.*;
/**
 *
 * @author Mikhail Titov
 */
public class ServiceStateNodeTest extends RavenCoreTestCase
{
    private ServiceStateNode stateNode;
    private PushDataSource ds;

    @Before
    public void prepare()
    {
        stateNode = new ServiceStateNode();
        stateNode.setName("state node");
        tree.getRootNode().addAndSaveChildren(stateNode);
        assertTrue(stateNode.start());

        ds = new PushDataSource();
        ds.setName("ds");
        tree.getRootNode().addAndSaveChildren(ds);
        assertTrue(ds.start());
    }

    @Test
    public void serviceAvailabilityControlTest() throws Exception
    {
        createControl("sa", new ServiceAvailabilityControlNode());
        assertNull(stateNode.getServiceAvailability());
        assertNull(stateNode.getServiceAvailabilityAlarm());

        ds.pushData(new Byte((byte)100));
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
        assertEquals(ServiceStateAlarm.NO_ALARM, stateNode.getServiceAvailabilityAlarm());
    }

    @Test
    public void serviceQualityControlTest() throws Exception
    {
        createControl("sa", new ServiceQualityControlNode());
        assertNull(stateNode.getServiceQuality());
        assertNull(stateNode.getServiceQualityAlarm());

        ds.pushData(new Byte((byte)100));
        assertEquals(new Byte((byte)100), stateNode.getServiceQuality());
        assertEquals(ServiceStateAlarm.NO_ALARM, stateNode.getServiceQualityAlarm());
    }

    @Test
    public void serviceStabilityControlTest() throws Exception
    {
        createControl("sa", new ServiceStabilityControlNode());
        assertNull(stateNode.getServiceStability());
        assertNull(stateNode.getServiceStabilityAlarm());

        ds.pushData(new Byte((byte)100));
        assertEquals(new Byte((byte)100), stateNode.getServiceStability());
        assertEquals(ServiceStateAlarm.NO_ALARM, stateNode.getServiceStabilityAlarm());
    }

    @Test
    public void compositeServiceTest()
    {
        createChildStateService(
                "service1", new Byte[]{(byte)100, (byte)50, (byte)0}
                , new Float[]{(float)1., (float)1., (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, WARNING, CRITICAL});
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
        assertEquals(NO_ALARM, stateNode.getServiceAvailabilityAlarm());

        assertEquals(new Byte((byte)50), stateNode.getServiceQuality());
        assertEquals(WARNING, stateNode.getServiceQualityAlarm());
        
        assertEquals(new Byte((byte)0), stateNode.getServiceStability());
        assertEquals(CRITICAL, stateNode.getServiceStabilityAlarm());
    }

    @Test
    public void weightCompositionTest1()
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)100}
                , new Float[]{(float)1., (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
    }

    @Test
    public void weightCompositionTest2()
    {
        createChildStateServices(
                new Byte[]{(byte)50, (byte)50}
                , new Float[]{(float)1., (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)50), stateNode.getServiceAvailability());
    }

    @Test
    public void weightCompositionTest3()
    {
        createChildStateServices(
                new Byte[]{(byte)50, (byte)50}
                , new Float[]{(float)1., (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)50), stateNode.getServiceAvailability());
    }

    @Test
    public void weightCompositionTest4()
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50}
                , new Float[]{(float).5, (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)67), stateNode.getServiceAvailability());
    }

    @Test
    public void weightCompositionTest5()
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50, (byte)70}
                , new Float[]{(float).5, (float)1., (float).5}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)68), stateNode.getServiceAvailability());
    }

    @Test
    public void greaterThanOneWeightTest1() throws Exception
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50, (byte)70}
                , new Float[]{(float).5, (float)1., (float)1.1}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)67), stateNode.getServiceAvailability());
        
    }

    @Test
    public void greaterThanOneWeightTest2() throws Exception
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50, (byte)60}
                , new Float[]{(float).5, (float)1., (float)1.1}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM, NO_ALARM});
        assertEquals(new Byte((byte)60), stateNode.getServiceAvailability());
    }

    @Test
    public void alarmTest1() throws Exception
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50}
                , new Float[]{(float).5, (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, WARNING});
        assertEquals(WARNING, stateNode.getServiceAvailabilityAlarm());
    }

    @Test
    public void alarmTest2() throws Exception
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50}
                , new Float[]{(float).5, (float)1.}
                , new ServiceStateAlarm[]{CRITICAL, WARNING});
        assertEquals(CRITICAL, stateNode.getServiceAvailabilityAlarm());
    }

    @Test
    public void controlWithChildServiceTest() throws Exception
    {
        createControl("sa", new ServiceAvailabilityControlNode());
        assertNull(stateNode.getServiceAvailability());
        assertNull(stateNode.getServiceAvailabilityAlarm());

        createChildStateService(
                "service1", new Byte[]{(byte)50, (byte)50, (byte)0}
                , new Float[]{(float)1., (float)1., (float)1.}
                , new ServiceStateAlarm[]{WARNING, WARNING, CRITICAL});
        
        ds.pushData(new Byte((byte)100));
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
        assertEquals(ServiceStateAlarm.NO_ALARM, stateNode.getServiceAvailabilityAlarm());
    }

    @Test
    public void stopedChildServiceTest()
    {
        createChildStateServices(
                new Byte[]{(byte)100, (byte)50, (byte)100}
                , new Float[]{(float).5, (float)1., (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, NO_ALARM, NO_ALARM});
        stateNode.getChildren("child state 1").stop();
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
    }

    @Test
    public void refreshIntervalTest() throws Exception
    {
        ServiceStateNode.REFRESH_INTERVAL = 500;
        createChildStateService(
                "service1", new Byte[]{(byte)100, (byte)50, (byte)0}
                , new Float[]{(float)1., (float)1., (float)1.}
                , new ServiceStateAlarm[]{NO_ALARM, WARNING, CRITICAL});
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
        assertEquals(NO_ALARM, stateNode.getServiceAvailabilityAlarm());

        ServiceStateNode childState = (ServiceStateNode) stateNode.getChildren("service1");
        childState.setServiceAvailability((byte)50);
        assertEquals(new Byte((byte)100), stateNode.getServiceAvailability());
        Thread.sleep(501);
        assertEquals(new Byte((byte)50), stateNode.getServiceAvailability());
    }

    private void createControl(String name, ServiceStateControlNode stateControl) throws Exception
    {
        stateControl.setName(name);
        stateNode.addAndSaveChildren(stateControl);
        stateControl.setDataSource(ds);
        stateControl.getNodeAttribute(
                ServiceStateControlNode.STATE_EXPRESSION_ATTRIBUTE).setValue("data");
        assertTrue(stateControl.start());
    }

    private void createChildStateService(
            String name, Byte[] states, Float[] weights, ServiceStateAlarm[] alarms)
    {
        ServiceStateNode childState = new ServiceStateNode();
        childState.setName(name);
        stateNode.addAndSaveChildren(childState);

        childState.setServiceAvailability(states[0]);
        childState.setServiceAvailabilityWeight(weights[0]);
        childState.setServiceAvailabilityAlarm(alarms[0]);

        childState.setServiceQuality(states[1]);
        childState.setServiceQualityWeight(weights[1]);
        childState.setServiceQualityAlarm(alarms[1]);

        childState.setServiceStability(states[2]);
        childState.setServiceStabilityWeight(weights[2]);
        childState.setServiceStabilityAlarm(alarms[2]);

        assertTrue(childState.start());
    }

    private void createChildStateServices(
            Byte[] states, Float[] weights, ServiceStateAlarm[] alarms)
    {
        for (int i=0; i<states.length; ++i)
        {
            ServiceStateNode state = new ServiceStateNode();
            state.setName("child state "+i);
            stateNode.addAndSaveChildren(state);
            state.setServiceAvailability(states[i]);
            state.setServiceAvailabilityWeight(weights[i]);
            state.setServiceAvailabilityAlarm(alarms[i]);
            assertTrue(state.start());
        }
    }
}