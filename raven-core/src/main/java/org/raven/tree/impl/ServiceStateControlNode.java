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
import javax.script.Bindings;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ServiceStateAlarm;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ServiceStateControlNode extends BaseNode implements DataConsumer
{
    public enum StateType {SERVICE_AVAILABILITY, SERVICE_QUALITY, SERVICE_STABILITY}

    public final static String STATE_EXPRESSION_ATTRIBUTE = "stateExpression";

    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Byte stateExpression;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private DataSource dataSource;

    @NotNull @Parameter(defaultValue="100")
    private Byte noAlarmLevel;

    @NotNull @Parameter(defaultValue="50")
    private Byte warningLevel;
    
    private final StateType stateType;

    private DataSource currentDataSource;
    private Object currentData;

    public ServiceStateControlNode(StateType stateType)
    {
        this.stateType = stateType;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public Byte getStateExpression()
    {
        return stateExpression;
    }

    public void setStateExpression(Byte stateExpression)
    {
        this.stateExpression = stateExpression;
    }

    public Byte getNoAlarmLevel()
    {
        return noAlarmLevel;
    }

    public void setNoAlarmLevel(Byte noAlarmLevel)
    {
        this.noAlarmLevel = noAlarmLevel;
    }

    public Byte getWarningLevel()
    {
        return warningLevel;
    }

    public void setWarningLevel(Byte warningLevel)
    {
        this.warningLevel = warningLevel;
    }

    public synchronized void setData(DataSource dataSource, Object data)
    {
        if (!Status.STARTED.equals(getStatus()))
            return;
        currentDataSource = dataSource;
        currentData = data;
        try
        {
            Byte stateValue = stateExpression;
            ServiceStateAlarm alarm;
            if (stateValue==null)
                alarm = null;
            else if (stateValue>=noAlarmLevel)
                alarm = ServiceStateAlarm.NO_ALARM;
            else if (stateValue>=warningLevel)
                alarm = ServiceStateAlarm.WARNING;
            else
                alarm = ServiceStateAlarm.CRITICAL;
            ServiceStateNode serviceState = (ServiceStateNode) getParent();
            switch(stateType)
            {
                case SERVICE_AVAILABILITY :
                    serviceState.setServiceAvailability(stateValue);
                    serviceState.setServiceAvailabilityAlarm(alarm);
                    break;
                case SERVICE_QUALITY:
                    serviceState.setServiceQuality(stateValue);
                    serviceState.setServiceQualityAlarm(alarm);
                    break;
                case SERVICE_STABILITY:
                    serviceState.setServiceStability(stateValue);
                    serviceState.setServiceStabilityAlarm(alarm);
                    break;
            }
        }
        finally
        {
            currentData = null;
            currentDataSource = null;
        }
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes)
    {
        return null;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindings.put("data", currentData);
        bindings.put("dataSource", currentDataSource);
        bindings.put(ExpressionAttributeValueHandler.ENABLE_SCRIPT_EXECUTION_BINDING, true);
    }
}