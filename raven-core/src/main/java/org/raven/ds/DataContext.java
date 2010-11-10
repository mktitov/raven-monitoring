/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.ds;

import java.util.Collection;
import java.util.Map;
import org.raven.auth.UserContext;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
public interface DataContext
{
    /**
     * Returns the context parameters
     */
    public Map getParameters();
    /**
     * Access to the context parameters from groovy scripts
     */
    public Object getAt(String parameterName);
    /**
     * Access to the context parameters from groovy scripts
     */
    public void putAt(String parameterName, Object value);
    /**
     * Put the parameter linked with node. The same as
     * <pre>
     *  getParameter().put(node.getId()+"_"+parameterName, value)
     * </pre>
     * @param node the node with which the parameter is linked
     * @param parameterName the parameter name
     * @param value the value of the parameter
     */
    public void putNodeParameter(Node node, String parameterName, Object value);
    /**
     * Gets the value of the parameter linked with the node
     */
    public Object getNodeParameter(Node node, String parameterName);
    /**
     * Remove the parameter linked with node from context parameters
     * @return the value of r the removed parameter
     */
    public Object removeNodeParameter(Node node, String parameterName);
    /**
     * Adds error to the errors list
     * @param path the path to the node where error occurs
     * @param error eth error message
     */
    public void addError(String path, String error);
    /**
     * Returns the list of all errors
     */
    public Collection<String> getErrors();
    /**
     * Returns the session attributes or null. Returned map is editable
     */
    public Map<String, NodeAttribute> getSessionAttributes();
    /**
     * Adds new session attribute.
     * @param attr the attribute that must be added to the session attributes
     */
    public void addSessionAttribute(NodeAttribute attr);
    /**
     * Adds attributes passed in the parameter to session attributes.
     * @param attrs The collection of the attributes that must be added to the session attributes of
     *      the data context. The value of this parameter can be <b>null</b>
     */
    public void addSessionAttributes(Collection<NodeAttribute> attrs);
    /**
     * Adds attributes passed in the parameter to session attributes.
     * @param attrs The collection of the attributes that must be added to the session attributes of
     *      the data context. The value of this parameter can be <b>null</b>
     * @param replace If <b>true</b> attributes passed in parameters will replace attributes in context
     *      with the same name.
     */
    public void addSessionAttributes(Collection<NodeAttribute> attrs, boolean replace);
    /**
     * Returns the context of the user initiated data processing or null if data processing
     * was initiated by the system
     */
    public UserContext getUserContext();
}
