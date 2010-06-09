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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.api.NodeAccess;
import org.raven.api.impl.NodeAccessImpl;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.DataContextImpl;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class NodeScannerNode extends BaseNode implements DataSource, Schedulable, Viewable
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private Node startingPoint;

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull
    private Scheduler scheduler;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    private Boolean subtreeFilter;
    
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    private Boolean nodeFilter;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE, defaultValue="0")
    private Object nodeWeight;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean sortByNodeWeight;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean reverseOrder;

    @Parameter(defaultValue="0")
    @NotNull
    private Integer maxRowCount;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE, defaultValue="null")
    private Object includeAdditionalNodes;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean excludeScannedNode;

    private Node scanningNode;

    private TableImpl table;

    public void executeScheduledJob(Scheduler scheduler)
    {
        scannNodes();
    }
    
    public synchronized void scannNodes()
    {
		if (isLogLevelEnabled(LogLevel.DEBUG))
			debug("Starting scanning subtree - "+startingPoint.getPath());
        Scanner scanner = new Scanner();
        tree.scanSubtree(startingPoint, scanner, ScanOptionsImpl.EMPTY_OPTIONS);
        Collection<NodeInfo> foundNodes = scanner.foundNodes;
        int counter = 0;
        int maxCount = maxRowCount<=0? Integer.MAX_VALUE : maxRowCount;
        table = new TableImpl(new String[]{"node", "node weight"});
        boolean _excludeScannedNode = excludeScannedNode;
        for (NodeInfo nodeInfo: foundNodes)
        {
            if (counter++>=maxCount)
                break;
            if (!_excludeScannedNode)
                table.addRow(new Object[]{nodeInfo.node, nodeInfo.nodeWeight});
            addAdditionalNodes(table, nodeInfo.node);
        }
        sendTableToConsumers(table);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Boolean getSubtreeFilter() {
        return subtreeFilter;
    }

    public void setSubtreeFilter(Boolean subtreeFilter) {
        this.subtreeFilter = subtreeFilter;
    }

    public Object getIncludeAdditionalNodes() {
        return includeAdditionalNodes;
    }

    public void setIncludeAdditionalNodes(Object includeAdditionalNodes) {
        this.includeAdditionalNodes = includeAdditionalNodes;
    }

    public Integer getMaxRowCount() {
        return maxRowCount;
    }

    public void setMaxRowCount(Integer maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public Boolean getNodeFilter() {
        return nodeFilter;
    }

    public void setNodeFilter(Boolean nodeFilter) {
        this.nodeFilter = nodeFilter;
    }

    public Object getNodeWeight() {
        return nodeWeight;
    }

    public void setNodeWeight(Object nodeWeight) {
        this.nodeWeight = nodeWeight;
    }

    public Boolean getSortByNodeWeight() {
        return sortByNodeWeight;
    }

    public void setSortByNodeWeight(Boolean sortByNodeWeight) {
        this.sortByNodeWeight = sortByNodeWeight;
    }

    public Boolean getReverseOrder() {
        return reverseOrder;
    }

    public void setReverseOrder(Boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public Node getStartingPoint() {
        return startingPoint;
    }

    public void setStartingPoint(Node startingPoint) {
        this.startingPoint = startingPoint;
    }

    public Boolean getExcludeScannedNode() {
        return excludeScannedNode;
    }

    public void setExcludeScannedNode(Boolean excludeScannedNode) {
        this.excludeScannedNode = excludeScannedNode;
    }

    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        return null;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);

        bindings.put("scanningNode", new NodeAccessImpl(scanningNode));
    }

    private void addAdditionalNodes(TableImpl table, Node node)
    {
        scanningNode = node;
        Object add = includeAdditionalNodes;
        if (add==null)
            return;
        if (add instanceof Collection)
        {
            for (Object obj: (Collection)add)
                addNodeToTable(table, obj);
        }
        else if (  add.getClass().isArray() 
                && !add.getClass().getComponentType().isPrimitive())
        {
            for (Object obj: (Object[])add)
                addNodeToTable(table, obj);
        } 
        else
            addNodeToTable(table, add);
    }

    private boolean addNodeToTable(TableImpl table, Object obj)
    {
		boolean result = true;
        if (obj instanceof Node)
            table.addRow(new Object[]{obj, null});
        else if (obj instanceof NodeAccess)
            table.addRow(new Object[]{((NodeAccess)obj).asNode(), null});
        else
            result = false;

		if (isLogLevelEnabled(LogLevel.DEBUG))
			debug("Additional node added to the result table: "+obj.toString());

        return result;
    }

    private void sendTableToConsumers(TableImpl table)
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes!=null)
            for (Node depNode: depNodes)
                if (depNode.getStatus()==Status.STARTED && depNode instanceof DataConsumer)
                    ((DataConsumer)depNode).setData(this, table, new DataContextImpl());
    }

    public Boolean getAutoRefresh()
    {
        return true;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (table==null)
            return null;

        ViewableObject object = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, table);

        return Arrays.asList(object);
    }

    private class Scanner implements ScannedNodeHandler
    {
        private final Collection<NodeInfo> foundNodes;
        private final boolean sort;
        private final int maxNodeCount;

        public Scanner()
        {
            sort = sortByNodeWeight;
            if (sort)
                if (reverseOrder)
                    foundNodes = new TreeSet<NodeInfo>(Collections.reverseOrder());
                else
                    foundNodes = new TreeSet<NodeInfo>();
            else
                foundNodes = new ArrayList<NodeInfo>();
            maxNodeCount = maxRowCount<=0? Integer.MAX_VALUE : maxRowCount;
        }

        public ScanOperation nodeScanned(Node node)
        {
            if (!sort && foundNodes.size()>maxNodeCount)
                return ScanOperation.STOP;

            scanningNode = node;

            Boolean _subtreeFilter = subtreeFilter;
            if (_subtreeFilter==null || !subtreeFilter)
                return ScanOperation.SKIP_NODE;

            Boolean _nodeFilter = nodeFilter;
            if (_nodeFilter!=null && _nodeFilter)
            {
                Object _nodeWeight = nodeWeight;
                if (_nodeWeight==null && sort)
                {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                    {
                        debug(String.format(
                                "Skipping scanning node (%s) because of null node weight"
                                , scanningNode.getPath()));
                    }
                }
                else
                {
                    if (isLogLevelEnabled(LogLevel.DEBUG))
                        debug(String.format(
                                "Adding node (%s) to table. Node weight (%s)"
                                , node.getPath(), _nodeWeight));
                    foundNodes.add(new NodeInfo(node, nodeWeight));
                }
            }

            return ScanOperation.CONTINUE;
        }
    }

    private class NodeInfo implements Comparable<NodeInfo>
    {
        private final Node node;
        private final Object nodeWeight;

        public NodeInfo(Node node, Object nodeWeight)
        {
            this.node = node;
            this.nodeWeight = nodeWeight;
        }

        public int compareTo(NodeInfo o)
        {
            if (!(nodeWeight instanceof Comparable))
                return -1;
            else if (o.nodeWeight==null)
                return 1;
            else
            {
                int res = ((Comparable)nodeWeight).compareTo(o.nodeWeight);
                if (res==0)
                    return 1;
                else
                    return res;
            }
        }
    }
}
