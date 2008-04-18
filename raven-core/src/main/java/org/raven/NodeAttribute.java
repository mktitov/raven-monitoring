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

/**
 * Holds information about node attribute
 * 
 * @author Mikhail Titov
 */
public interface NodeAttribute 
{
    /**
     * Returns an unique attribute name.
     */
    public String getName();
    /**
     * Return the value of the attribute.
     */
    public String getValue();
    /**
     * Returns <code>true</code> if value for this attribute is required.
     */
    public boolean isRequired();
    /**
     * Returns the default value of an attribute or null if the default value
     * not specified.
     */
    public String getDefaultValue();
    /**
     * Returns the type of the attribute.
     */
    public Class getType();
    /**
     * Returns the parent attribute name.
     */
    public String getParentAttribute();
}
