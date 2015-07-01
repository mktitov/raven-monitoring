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
 *
 * @author Mikhail Titov
 */
public interface ScanOptions
{
    /**
     * Returns array of the node types that will be scanned (only nodes of this types will
     * be passed to the {@link ScannedNodeHandler handler}).
     * <p/>
     * If method returns null or empty array then all nodes will be scanned.
     *
     * @see Tree#scanSubtree
     */
    public Class[] includeNodeTypes();
    /**
     * Returns array of the node statuses that will be scanned (only nodes with this statuses will
     * be passed to the {@link ScannedNodeHandler handler}).
     * <p/>
     * Empty array or null means any statuses.
     *
     * @see Tree#scanSubtree
     */
    public Node.Status[] includeStatuses();
    /**
     * If methods returns <b>true</b> then childrens of the scanning node will sorted before scan
     */
    public boolean sortBeforeScan();
}
