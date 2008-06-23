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

import org.weda.constraints.ReferenceValueCollection;
import org.weda.constraints.TooManyReferenceValuesException;

/**
 * Returns the reference values for {@link NodeAttribute node attribute}
 * @author Mikhail Titov
 */
public interface AttributeReferenceValues 
{
    /**
     * The method fills the collection <code>referenceValues</code> with reference values.
     * @param attr the node attribute for wich reference values are generating.
     * @param referenceValues the collection to which reference values will collect.
     * @return If methods returns <b>true</b> then the reference values collecting will stoped.
     */
    public boolean getReferenceValues(NodeAttribute attr, ReferenceValueCollection referenceValues)
            throws TooManyReferenceValuesException;
}
