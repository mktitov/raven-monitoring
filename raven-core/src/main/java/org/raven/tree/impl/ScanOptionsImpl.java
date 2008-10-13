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

package org.raven.tree.impl;

import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.ScanOptions;

/**
 *
 * @author Mikhail Titov
 */
public class ScanOptionsImpl implements ScanOptions
{
    public static ScanOptions EMPTY_OPTIONS = new ScanOptionsImpl();

    private Class[] nodeTypes = null;
    private Node.Status[] statuses = null;
    private boolean sortBeforeScan = false;

    public ScanOptionsImpl setNodeTypes(Class... nodeTypes)
    {
        this.nodeTypes = nodeTypes;
        return this;
    }

    public ScanOptionsImpl setStatuses(Node.Status... statuses)
    {
        this.statuses = statuses;
        return this;
    }

    public ScanOptionsImpl setSortBeforeScan(boolean sortBeforeScan)
    {
        this.sortBeforeScan = sortBeforeScan;
        return this;
    }

    public Class[] includeNodeTypes()
    {
        return nodeTypes;
    }

    public Status[] includeStatuses()
    {
        return statuses;
    }

    public boolean sortBeforeScan()
    {
        return sortBeforeScan;
    }
}
