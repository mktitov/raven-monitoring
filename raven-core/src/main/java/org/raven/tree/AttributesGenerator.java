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

import java.util.Collection;

/**
 * 
 * @author Mikhail Titov
 */
public interface AttributesGenerator 
{
    /**
     * Generates and adds attributes to the node passed in <code>owner</code> parameter. 
     * 
     * @param owner the owner of generated attributes
     * @param parentAttribute the name of the parent attribute
     * 
     * @see NodeAttribute#getOwner() 
     * @see NodeAttribute#getParentAttribute() 
     */
    public Collection<NodeAttribute> generateAttributes();
}
