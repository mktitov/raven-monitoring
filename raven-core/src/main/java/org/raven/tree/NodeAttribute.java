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

/**
 * Holds information about node attribute
 * 
 * @author Mikhail Titov
 */
public interface NodeAttribute 
{
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
     * Returns the parameter name or null if the attribute not linked with node logic parameter.
     */
    public String getParameterName();
    /**
     * Returns the short description of the attribute.
     */
    public String getDescription();
    /**
     * Return the value of the attribute.
     */
    public String getValue();
    /**
     * Returns the type of the attribute.
     */
    public Class getType();
    /**
     * Returns the parent attribute name.
     */
    public String getParentAttribute();
}
