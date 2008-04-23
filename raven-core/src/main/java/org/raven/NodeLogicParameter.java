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

package org.raven;

import org.weda.beans.PropertyDescriptor;

/**
 * Holds the node logic object parameter
 * 
 * @see Node#getNodeLogic() 
 * @see Node#getNodeLogicType() 
 * 
 * @author Mikhail Titov
 */
public interface NodeLogicParameter 
{
    /**
     * Returns the parameter name (the node logic class property name).
     */
    public String getName();
    /**
     * Returns the display name of the parameter.
     */
    public String getDisplayName();
    /**
     * Returns the short parameter description.
     */
    public String getDescription();
    /**
     * Returns the parameter type.
     */
    public Class getType();
    /**
     * Returns the parameter value.
     */
    public Object getValue();
    /**
     * Sets the parameter value.
     */
    public void setValue(Object value);
    /**
     * Returns the property descriptor of the parameter
     */
    public PropertyDescriptor getPropertyDescriptor();
}
