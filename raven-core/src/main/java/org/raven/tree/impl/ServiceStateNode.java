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

import java.util.Collection;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.ServiceState;
import org.raven.tree.ServiceStateAlarm;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class ServiceStateNode extends AbstractServiceStateNode
{
    public final static int REFRESH_INTERVAL = 10000;

    @Parameter
    private Byte serviceAvailability;
    
    @Parameter
    private Byte serviceQuality;
    
    @Parameter
    private Byte serviceStability;

    @Parameter
    private ServiceStateAlarm serviceAvailabilityAlarm;

    @Parameter
    private ServiceStateAlarm serviceQualityAlarm;

    @Parameter
    private ServiceStateAlarm serviceStabilityAlarm;

    private long lastRefreshTime;

    @Override
    protected void initFields()
    {
        super.initFields();
        lastRefreshTime = 0l;
    }

    private synchronized void refreshStates()
    {
        if (System.currentTimeMillis()-lastRefreshTime<=REFRESH_INTERVAL)
            return;
        Collection<Node> childs = getChildrens();
        if (childs!=null && childs.isEmpty())
        {
            StateCalc aCalc = new StateCalc();
            StateCalc qCalc = new StateCalc();
            StateCalc sCalc = new StateCalc();
            boolean hasAvailibilityControl = false;
            boolean hasQualityControl = false;
            boolean hasStabilityControl = false;
            for (Node child: childs)
            {
                if (!Status.STARTED.equals(child.getStatus()))
                    continue;
                if (child instanceof ServiceAvailabilityControlNode)
                    hasAvailibilityControl = true;
                else if (child instanceof ServiceQualityControlNode)
                    hasQualityControl = true;
                else if (child instanceof ServiceStabilityControlNode)
                    hasStabilityControl = true;
                else if (child instanceof ServiceState)
                {
                    ServiceState state = (ServiceState) child;
                    aCalc.aggregate(
                            state.getServiceAvailability(), state.getServiceAvailabilityWeight()
                            , state.getServiceAvailabilityAlarm());
                    qCalc.aggregate(
                            state.getServiceQuality(), state.getServiceQualityWeight()
                            , state.getServiceQualityAlarm());
                    sCalc.aggregate(
                            state.getServiceStability(), state.getServiceStabilityWeight()
                            , state.getServiceStabilityAlarm());
                }
            }
            if (!hasAvailibilityControl)
            {
                setServiceAvailability(aCalc.getState());
                setServiceAvailabilityAlarm(aCalc.getAlarm());
            }
            if (!hasQualityControl)
            {
                setServiceQuality(qCalc.getState());
                setServiceQualityAlarm(qCalc.getAlarm());
            }
            if (!hasStabilityControl)
            {
                setServiceStability(sCalc.getState());
                setServiceStabilityAlarm(sCalc.getAlarm());
            }
        }
    }

    public Byte getServiceAvailability()
    {
        refreshStates();
        return serviceAvailability;
    }

    public void setServiceAvailability(Byte serviceAvailability)
    {
        this.serviceAvailability = serviceAvailability;
    }

    public Byte getServiceQuality()
    {
        refreshStates();
        return serviceQuality;
    }

    public void setServiceQuality(Byte serviceQuality)
    {
        this.serviceQuality = serviceQuality;
    }

    public Byte getServiceStability()
    {
        refreshStates();
        return serviceStability;
    }

    public void setServiceStability(Byte serviceStability)
    {
        this.serviceStability = serviceStability;
    }

    public ServiceStateAlarm getServiceAvailabilityAlarm()
    {
        refreshStates();
        return serviceAvailabilityAlarm;
    }

    public void setServiceAvailabilityAlarm(ServiceStateAlarm serviceAvailabilityAlarm)
    {
        this.serviceAvailabilityAlarm = serviceAvailabilityAlarm;
    }

    public ServiceStateAlarm getServiceQualityAlarm()
    {
        refreshStates();
        return serviceQualityAlarm;
    }

    public void setServiceQualityAlarm(ServiceStateAlarm serviceQualityAlarm)
    {
        this.serviceQualityAlarm = serviceQualityAlarm;
    }

    public ServiceStateAlarm getServiceStabilityAlarm()
    {
        refreshStates();
        return serviceStabilityAlarm;
    }

    public void setServiceStabilityAlarm(ServiceStateAlarm serviceStabilityAlarm)
    {
        this.serviceStabilityAlarm = serviceStabilityAlarm;
    }

    private static class StateCalc
    {
        private float wSum=0;
        private int wCount = 0;
        private byte min = 100;
        private double sum = 0.;
        private ServiceStateAlarm maxAlarm = ServiceStateAlarm.NO_ALARM;

        public void aggregate(Byte state, Float weight, ServiceStateAlarm alarm)
        {
            float w = weight.floatValue();
            byte a = state==null? 100 : state;
            if (w<=1)
            {
                ++wCount;
                wSum += w;
                sum += a*w;
            }
            else
                min = (byte) Math.min(min, a);
            maxAlarm = maxAlarm(maxAlarm, alarm);
        }

        public byte getState()
        {
            byte state = (byte) Math.round(sum / wSum);
            return state<min? state : min;
        }

        public ServiceStateAlarm getAlarm()
        {
            return maxAlarm;
        }

        private static ServiceStateAlarm maxAlarm(ServiceStateAlarm a1, ServiceStateAlarm a2)
        {
            return a2==null || a1.ordinal()>=a2.ordinal()? a1 : a2;
        }
    }
}
