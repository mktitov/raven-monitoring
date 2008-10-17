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

package org.raven.log;

import java.util.Date;
import java.util.List;

import org.raven.log.impl.NodeLoggerNode;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface NodeLogger
{
    void setNodeLoggerNode(NodeLoggerNode nodeLoggerNode);
    NodeLoggerNode getNodeLoggerNode();
    
    public void write(Node node, LogLevel level, String message);
    
    public List<NodeLogRecord> getRecords(Date from,Date to, Integer nodeId, LogLevel level);
    
}
