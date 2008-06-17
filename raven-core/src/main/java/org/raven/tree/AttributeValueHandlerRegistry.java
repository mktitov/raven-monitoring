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
import org.weda.constraints.ReferenceValue;

/**
 * The registry of {@link AttributeValueHandler value handler}s
 * @author Mikhail Titov
 */
public interface AttributeValueHandlerRegistry 
{
    /**
     * Returns the list of the value handler types available for the attribute.
     * Method {@link ReferenceValue#getValue()} returns the value handler type
     * and the method {@link ReferenceValue#getValueAsString()} returns the 
     * {@link AttributeValueHandlerFactory#getName() localized name}
     * of the value handler.
     */
    public List<ReferenceValue> getValueHandlerTypes();
    /**
     * Returns the value handler by its type.
     * @param valueHandlerType the value handler type.
     * @param attribute node attribute for which value handler is creating.
     * @throws AttributeValueHandlerRegistryError if registry does not contains the 
     *      {@link AttributeValueHandlerFactory} for the type passed in the 
     *      <code>valueHandlerType</code> parameter.
     */
    public AttributeValueHandler getValueHandler(String valueHandlerType, NodeAttribute attribute);
}
