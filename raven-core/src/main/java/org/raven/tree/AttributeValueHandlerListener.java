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
 * The listener of the {@link AttributeValueHandler}
 * @author Mikhail Titov
 */
public interface AttributeValueHandlerListener 
{
    /**
     * Informs the listener that the {@link AttributeValueHandler#handleValue() value} changed.
     */
    public void valueChanged(Object oldValue, Object newValue);
    /**
     * Informs listener that expression in the value handler is invalidated.
     */
    public void expressionInvalidated(Object oldValue);
}
