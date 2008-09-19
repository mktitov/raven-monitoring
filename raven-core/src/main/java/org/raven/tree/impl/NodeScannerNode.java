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
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class NodeScannerNode extends BaseNode implements DataSource
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private Node startingPoint;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    @NotNull
    private Boolean subtreeFilter;
    
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    @NotNull
    private Boolean nodeFilter;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE, defaultValue="0")
    @NotNull
    private Object nodeWeight;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean sortByNodeWeight;

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean reverseOrder;

    @Parameter(defaultValue="0")
    @NotNull
    private int maxRowCount;

    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE, defaultValue="null")
    private Object includeAdditionalNodes;

    private Node scanningNode;

    public synchronized void scannNodes()
    {
        Scanner scanner = new Scanner();
        tree.scanSubtree(startingPoint, scanner, null);
        Collection<NodeInfo> foundNodes = scanner.foundNodes;
        int counter = 0;
        int maxCount = maxRowCount<=0? Integer.MAX_VALUE : maxRowCount;
        TableImpl table = new TableImpl(new String[]{"node", "node weight"});
        for (NodeInfo nodeInfo: foundNodes)
        {
            if (counter++>=maxCount)
                break;
            table.addRow(new Object[]{nodeInfo.node, nodeInfo.nodeWeight});
            addAdditionalNodes(table, nodeInfo.node);
        }
        sendTableToConsumers(table);
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

    public int getMaxRowCount() {
        return maxRowCount;
    }

    public void setMaxRowCount(int maxRowCount) {
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

    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes) 
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

        bindings.put("scanningNode", scanningNode);
    }

    private void addAdditionalNodes(TableImpl table, Node node)
    {
        scanningNode = node;
        Object add = includeAdditionalNodes;
        if (add==null)
            return;
        if (add instanceof Node)
            table.addRow(new Object[]{add, null});
        else if (add instanceof Collection)
        {
            for (Object obj: (Collection)add)
                if (obj instanceof Node)
                    table.addRow(new Object[]{obj, null});
        }
        else if (  add.getClass().isArray() 
                && !add.getClass().getComponentType().isPrimitive())
        {
            for (Object obj: (Object[])add)
                if (obj instanceof Node)
                    table.addRow(new Object[]{obj, null});
        }
    }

    private void sendTableToConsumers(TableImpl table)
    {
        Collection<Node> depNodes = getDependentNodes();
        if (depNodes!=null)
            for (Node depNode: depNodes)
                if (depNode.getStatus()==Status.STARTED && depNode instanceof DataConsumer)
                    ((DataConsumer)depNode).setData(this, table);
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

            if (!subtreeFilter)
                return ScanOperation.SKIP_NODE;

            if (nodeFilter)
                foundNodes.add(new NodeInfo(node, nodeWeight));

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
                return 1;
            else
                return ((Comparable)nodeWeight).compareTo(o.nodeWeight);
        }
    }
}
