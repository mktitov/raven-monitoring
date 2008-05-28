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

package org.raven.tree;

import java.util.List;
import org.weda.beans.PropertyDescriptor;
import org.weda.constraints.ConstraintException;
import org.weda.constraints.ReferenceValue;
import org.weda.constraints.TooManyReferenceValuesException;

/**
 * Holds the node logic object parameter
 * 
 * @see Node#getNodeLogic() 
 * @see Node#getNodeLogicType() 
 * 
 * @author Mikhail Titov
 */
public interface NodeParameter 
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
     * The format string that used to convert value to string and vice versa.
     */
    public String getPattern();
    /**
     * If returns <code>true</code> the value of this node parameter must be seted before 
     * {@link Node#start() node logic execution}.
     */
    public boolean isRequired();
    /**
     * Returns the parameter value.
     */
    public Object getValue();
    /**
     * Sets the parameter value.
     */
    public void setValue(Object value) throws ConstraintException;
    /**
     * Returns the property descriptor of the parameter
     */
    public PropertyDescriptor getPropertyDescriptor();
    /**
     * Returns the node attribute linked with the parameter.
     */
    public NodeAttribute getNodeAttribute();
    /**
     * Links the parameter with the node attribute.
     * @param nodeAttribute
     */
    public void setNodeAttribute(NodeAttribute nodeAttribute);
    /**
     * Returns the parameter reference values or <code>null</code> if parameter doesn't have
     * reference values.
     */
    public List<ReferenceValue> getReferenceValues() throws TooManyReferenceValuesException;
}
