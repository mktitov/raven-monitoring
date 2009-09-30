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

import java.util.ArrayList;
import java.util.List;
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
        List<Node> childs = getSortedChildrens();
        List<ServiceState> stateNodes = new ArrayList<ServiceState>(childs.size());
        boolean hasAvailibilityControl = false;
        boolean hasQualityControl = false;
        boolean hasStabilityControl = false;
        for (Node child: childs)
        {
            if (child instanceof ServiceAvailabilityControlNode)
                hasAvailibilityControl = true;
            else if (child instanceof ServiceQualityControlNode)
                hasQualityControl = true;
            else if (child instanceof ServiceStabilityControlNode)
                hasStabilityControl = true;
            else if (child instanceof ServiceState)
                stateNodes.add((ServiceState) child);
        }
        if (   (!hasAvailibilityControl || !hasQualityControl || !hasStabilityControl)
            && !stateNodes.isEmpty())
        {
            int count = stateNodes.size();
            float sa = 0;
            float sq = 0;
            float st = 0;
            byte value; Byte state;
            ServiceStateAlarm saAlarm = ServiceStateAlarm.NO_ALARM;
            ServiceStateAlarm sqAlarm = ServiceStateAlarm.NO_ALARM;
            ServiceStateAlarm stAlarm = ServiceStateAlarm.NO_ALARM;
            for (ServiceState serviceState: stateNodes)
            {
                if (!hasAvailibilityControl)
                {
                    state = serviceState.getServiceAvailability();
                    value = state==null? 0 : state;
                    sa += ((float)value/count)*serviceState.getServiceAvailabilityWeight();
                    saAlarm = maxAlarm(saAlarm, serviceState.getServiceAvailabilityAlarm());
                }
                if (!hasQualityControl)
                {
                    state = serviceState.getServiceQuality();
                    value = state==null? 0 : state;
                    sq += ((float)value/count)*serviceState.getServiceQualityWeight();
                    sqAlarm = maxAlarm(sqAlarm, serviceState.getServiceQualityAlarm());
                }
                if (!hasStabilityControl)
                {
                    state = serviceState.getServiceStability();
                    value = state==null? 0 : state;
                    st += ((float)value/count)*serviceState.getServiceStabilityWeight();
                    stAlarm = maxAlarm(stAlarm, serviceState.getServiceStabilityAlarm());
                }
            }
            if (!hasAvailibilityControl)
            {
                serviceAvailability = (byte)Math.round(sa);
                serviceAvailabilityAlarm = saAlarm;
            }
            if (!hasQualityControl)
            {
                serviceQuality = (byte)Math.round(sq);
                serviceQualityAlarm = sqAlarm;
            }
            if (!hasStabilityControl)
            {
                serviceStability = (byte)Math.round(st);
                serviceStabilityAlarm = stAlarm;
            }
        }
    }

    private ServiceStateAlarm maxAlarm(ServiceStateAlarm a1, ServiceStateAlarm a2)
    {
        return a2==null || a1.ordinal()>=a2.ordinal()? a1 : a2;
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
}
