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

package org.raven.api;

import java.util.Map;
import org.raven.ds.ArchiveException;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.table.DataArchiveTable;
import org.raven.tree.Node;

/**
 * Stricted access to the {@link org.raven.tree.Node}.
 * @author Mikhail Titov
 */
public interface NodeAccess 
{
    public int getId();
    public int getIndex();
    public NodeAccess getParent();
    public String getName();
    public String getPath();
    public Map<String, NodeAccess> getChilds();
//    public Map<String, NodeAttributeAccess> getAttrs();
    public NodeAttributeAccess getAttr(String attributeName);
    public NodeAttributeAccess getAt(String attributeName);
    public Node asNode();
    public Map<String, Object> getVars();
    public NodeAttributeAccess getParentAttr(String attributeName);

    public RRGraphNode findGraph();
    public DataArchiveTable getArchivedData(String fromDate, String toDate) throws ArchiveException;
}
