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
import org.weda.constraints.ConstraintException;

/**
 * Holds information about node attribute
 * 
 * @author Mikhail Titov
 */
//TODO: add isVisible method
//TODO: add isReadOnly functionality
public interface NodeAttribute extends Cloneable
{
    /**
     * Gets the unique attribute id.
     */
    public int getId();
    /**
     * Sets the unique attribute id.
     */
    public void setId(int id);
    /**
     * Returns the owner node of the attribute
     */
    public Node getOwner();
    /**
     * Sets the attribute owner.
     */
    public void setOwner(Node owner);
    /**
     * Returns an unique attribute name.
     */
    public String getName();
    /**
     * Sets the attribute name. The name must be unique.
     */
    public void setName(String name);
    /**
     * Returns the parameter name or null if the attribute not linked with node logic parameter.
     */
    public String getParameterName();
    /**
     * Returns the short description of the attribute.
     */
    public String getDescription();
    /**
     * Sets the attribute description.
     */
    public void setDescription(String description);
    /**
     * If returns <code>true</code> if the value of this node attribute must be seted before 
     * {@link Node#start() node logic execution}.
     */
    public boolean isRequired();
    /**
     * 
     */
    public void setRequired(boolean required);
    /**
     * Method returns:
     * <ul>
     *  <li>The value of the attribute converted to the {@link #getType() attribute type}. 
     *      If the attribute is linked with {@link #getParameterName() node parameter} 
     *      then the value is the parameter value.
     *  <li>If the value is null then the real value of the attribute with the same name 
     *      of the nearest parent node.
     *  </li>
     *  <li>Otherwise the method returns null.
     * </ul>
     * @see Node#getParentAttributeRealValue(java.lang.String) 
     */
    public <T> T getRealValue();
    /**
     * Method returns:
     * <ul>
     *  <li>The value of the attribute. If the attribute is linked with 
     *      {@link #getParameterName() node parameter} then the value is the parameter value
     *      converted to string.
     *  <li>If the value is null then the value of the attribute with the same name of the nearest
     *      parent node.
     *  </li>
     *  <li>Otherwise the method returns null.
     * </ul>
     * @see Node#getParentAttributeValue(java.lang.String) 
     */
    public String getValue();
    /**
     * Returns the raw value (the value without any transformation)
     * @see #getValue() 
     */
    public String getRawValue();
    /**
     * Sets the raw value (the value without any transformation)
     * @see #getRawValue() 
     * @see #getValue() 
     */
    public void setRawValue(String rawValue);
    /**
     * Sets the attribute value.
     */
    public void setValue(String value) throws ConstraintException;
    /**
     * Returns the type of the attribute.
     */
    public Class getType();
    /**
     * Sets the type of the attribute.
     */
    public void setType(Class type);
    /**
     * Returns the parent attribute name.
     */
    public String getParentAttribute();
    /**
     * Sets the name of the parent attribute.
     */
    public void setParentAttribute(String name);
    /**
     * Returns the list of the values that this attribute can take. If method returns null
     * then the attribute does not have reference values.
     */
    public List<String> getReferenceValues();
    /**
     * Returns <code>true</code> if attribute type is {@link AttributesGenerator}
     */
    public boolean isGeneratorType();
    /**
     * Returns true if this attribute is references to the other node attribute.
     * @see #getAttributeReference()
     */
    public boolean isAttributeReference();
    /**
     * Returns reference to the attribute.
     * @see #isAttributeReference() 
     */
    public AttributeReference getAttributeReference();
    /**
     * Clones the attribute.
     * @throws java.lang.CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException;
}
