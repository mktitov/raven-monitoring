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

package org.raven.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark the class as node class. The class must implements the {@link org.raven.tree.Node} 
 * interface.
 * @author Mikhail Titov
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NodeClass 
{
    /**
     * If this parameter sets to <code>true</code> then annotated node can hold child nodes 
     * which {@link #parentNode()} parameter sets to the <code>Void.class</code>.
     */
    boolean anyChildTypes() default false;
    /**
     * If parameter seted then annotated node can be added only to the node with specified type.
     */
    Class parentNode() default Void.class;
    Class[] childNodes() default Void.class;
    /**
     * If this parameter sets to <code>true</code> then the parent node child nodes will be added
     * to the list of the child node types.
     */
    boolean importChildTypesFromParent() default false;
    /**
     * Imports child node types from parent of level passed in the parameter. For instance, if
     * parameter value is 1 then child node types will be imported from direct parent (Node.getParent()),
     * if value equals to 2 then child node types will be imported from parent of direct parent 
     * (node.getParent().getParent())
     */
    int importChildTypesFromParentLevel() default 0;
    /**
     * Imports child node types from the node passed in the parameter
     */
    Class importChildTypesFrom() default Void.class;
}
