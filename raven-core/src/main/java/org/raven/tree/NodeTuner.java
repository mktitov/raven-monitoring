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
 * Allows to new tune node parameters in the clone process.
 * 
 * @author Mikhail Titov
 */
public interface NodeTuner 
{
    /**
     * If method returns not null value then node returned by this method will be used as clone
     * of the <code>sourceNode</code>.
     * @param sourceNode the node that must be cloned.
     */
    public Node cloneNode(Node sourceNode);
    /**
     * Tune cloned node.
     * @param sourceNode the node which was cloned.
     * @param sourceClone the clone of the <code>sourceNode</code>
     */
    public void tuneNode(Node sourceNode, Node sourceClone);
    /**
     * Executing after initialization cloned node.
     * @param sourceClone the clone node
     */
    public void finishTuning(Node sourceClone);
}
