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

package org.raven.tree;

/**
 * Holds the state of ther service. The state consists of three characteristics:
 * <ul>
 *  <li>Service availability</li>
 *  <li>Service quality</li>
 *  <li>Service stability</li>
 * </ul>
 * @author Mikhail Titov
 */
public interface ServiceState extends Node
{
    /**
     * Returns the service availability in percentage or null if the state is unknown.
     * The value must be in range [0..100] or null.
     */
    public Byte getServiceAvailability();
    /**
     * Returns the weight of the service availability characteristics in relation to sibling service
     * states. The value must be in range [0..1]
     */
    public Float getServiceAvailabilityWeight();
    /**
     * Returns the alarm of the service availability state
     */
    public ServiceStateAlarm getServiceAvailabilityAlarm();
    /**
     * Returns the service quality in percentage or null if the state is unknown.
     * The value must be in range [0..100] or null.
     */
    public Byte getServiceQuality();
    /**
     * Returns the alarm of the service quality state
     */
    public ServiceStateAlarm getServiceQualityAlarm();
    /**
     * Returns the weight of the service quality characteristics in relation to sibling service
     * states. The value must be in range [0..1]
     */
    public Float getServiceQualityWeight();
    /**
     * Returns the service stability in percentage or null if the state is unknown.
     * The value must be in range [0..100] or null.
     */
    public Byte getServiceStability();
    /**
     * Returns the alarm of the service stability state
     */
    public ServiceStateAlarm getServiceStabilityAlarm();
    /**
     * Returns the weight of the service stability characteristics in relation to sibling service
     * states. The value must be in range [0..1]
     */
    public Float getServiceStabilityWeight();
}
