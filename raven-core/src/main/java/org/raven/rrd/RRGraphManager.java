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

package org.raven.rrd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.graph.RRDef;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.template.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodePathResolver;
import org.raven.tree.NodeTuner;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.raven.tree.impl.AttributeReferenceValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeListenerAdapter;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 * Allows to create {@link org.raven.rrd.graph.RRGraphNode graphics} 
 * @author Mikhail Titov
 */
@NodeClass()
public class RRGraphManager extends BaseNode
{
    public final static String STARTINGPOINT_ATTRIBUTE = "startingPoint";
    public final static String FILTER_EXPRESSION_ATTRIBUTE = "filterExpression";
    public final static long LOCK_TIMEOUT = 500;
    
    private static NodePathResolver pathResolver;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @Description("The node from which graph manager take a control on dataSources")
    @NotNull
    private Node startingPoint;
    
    @Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
    @NotNull
    @Description(
        "The expression that will be call on each dataSource. " +
        "If expression returns true then dataSource will be added to the graph. " +
        "The (dataSource) parameter will be in the expression context")
    private Boolean filterExpression;
    
    private RRGraphManagerTemplate template;
    private Lock lock;
    private Map<DataSource, RRDef> dataSources;
    private Map<String, Object> expressionContext;
    private StartingPointListener startingPointListener;

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantLock();
        dataSources = new HashMap<DataSource, RRDef>();
        expressionContext = new HashMap<String, Object>();
        startingPointListener = new StartingPointListener();
    }

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        
        template = (RRGraphManagerTemplate) getChildren(RRGraphManagerTemplate.NAME);
        if (template==null)
        {
            template = new RRGraphManagerTemplate();
            addChildren(template);
            template.save();
            template.init();
        }
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        sync();
    }

    @Override
    public synchronized void stop() throws NodeError 
    {
        if (startingPoint!=null)
            startingPoint.removeListener(startingPointListener);
        dataSources.clear();
        super.stop();
    }

    public RRGraphManagerTemplate getTemplate() {
        return template;
    }

    public Node getStartingPoint()
    {
        return startingPoint;
    }

    public Boolean getFilterExpression() {
        return filterExpression;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindings.putAll(expressionContext);
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue) 
    {
        if (   getStatus()==Status.STARTED 
            && node==this 
            && attribute.getName().equals(STARTINGPOINT_ATTRIBUTE))
        {
            if (oldValue!=null)
                ((Node)oldValue).removeListener(startingPointListener);
            if (newValue!=null)
                ((Node)newValue).addListener(startingPointListener);
        }
    }

    private void addDataSourceToGraph(DataSource dataSource) 
    {
        RRDataSource rrds = getRRDataSourceForDataSource(dataSource);

        if (rrds==null)
            return;

        expressionContext.put("dataSource", dataSource);
        expressionContext.put("rrDataSource", rrds);
        
        if (filterExpression==null || filterExpression.equals(Boolean.FALSE))
            return;
        
        Collection<Node> tempChilds = template.getEffectiveChildrens();
        if (tempChilds==null || tempChilds.size()==0)
           return;
        
        Node gTemplate = tempChilds.iterator().next();
        Node injectingPoint = this;
        while (true)
        {
            if (gTemplate instanceof GroupNode || gTemplate instanceof RRGraphNode)
            {
                NodeAttribute groupExpr = 
                        gTemplate.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
                String groupName = groupExpr.getValue();
                if (groupName==null)
                {
                    logger.error(String.format(
                            "Error adding graph for dataSource (%s) groupingExpression attribute " +
                            "of the node (%s) returns NULL"));
                    return;
                }
                Node nextInjectingPoint = injectingPoint.getChildren(groupName);
                if (nextInjectingPoint==null)
                {
                    break;
                }else
                {
                    injectingPoint = nextInjectingPoint;
                    Collection<Node> childs = gTemplate.getChildrens();
                    if (childs==null || childs.size()==0)
                    {
                        logger.error(String.format(
                                "Error adding graph for dataSource (%s). " +
                                "The template node (%s) must have a child nodes"
                                , dataSource, gTemplate));
                        return;
                    }
                    gTemplate = childs.iterator().next();
                }
            }
            else
                break;
        }
        
        NodeTuner tuner = new Tuner(dataSource, rrds);
        if (gTemplate instanceof GroupNode || gTemplate instanceof RRGraphNode)
        {
            //cloning subtree
            tree.copy(gTemplate, injectingPoint, null, tuner, true, false);
        }
        else
        {
            //cloning all nodes inside graph template (the parent of the gTemplate)
            Collection<Node> childs = gTemplate.getParent().getChildrens();
            NodeAttribute autoColorAttr = incrementAutoColorAttribute(injectingPoint);
            RRColor nextColor = autoColorAttr.getRealValue();
            
            if (childs!=null)
            {
                List<Node> clonedNodes = new ArrayList<Node>();
                for (Node node: childs)
                    clonedNodes.add(tree.copy(node, injectingPoint, null, tuner, true, false));
                
                for (Node node: clonedNodes)
                {
                    Collection<NodeAttribute> attrs = node.getNodeAttributes();
                    if (attrs!=null)
                        for (NodeAttribute attr: attrs)
                        {
                            revalidateAttributeExpression(attr, node);
                            if (   AttributeReferenceValueHandlerFactory.TYPE.equals(
                                     attr.getValueHandlerType())
                                && autoColorAttr==attr.getR)
                            {
                                
                            }
                        }
                }
            }
        }
    }

    private RRDataSource getRRDataSourceForDataSource(DataSource dataSource) 
    {
        RRDataSource rrds = null;
        Collection<Node> nodeDeps = dataSource.getDependentNodes();
        if (nodeDeps != null) {
            for (Node dep : nodeDeps) {
                if (dep instanceof RRDataSource) {
                    rrds = (RRDataSource) dep;
                    break;
                }
            }
        }
        return rrds;
    }

    private NodeAttribute incrementAutoColorAttribute(Node injectingPoint) throws NodeError 
    {
        NodeAttribute colorAttr = 
                injectingPoint.getNodeAttribute(RRGraphManagerTemplate.AUTOCOLOR_ATTRIBUTE);
        RRColor nextColor = RRColor.BLACK;
        if (colorAttr != null) {
            RRColor curColor = colorAttr.getRealValue();
            if (curColor != null) {
                nextColor = curColor.nextColor();
            }
        } else {
            colorAttr = getTemplate().addAutoColorAttribute(injectingPoint);
        }
        try {

            colorAttr.setValue(nextColor.toString());
            colorAttr.save();
        } catch (Exception ex) {
            logger.error(String.format(
                    "Error saving new color (%s) for attribute (%s) in node (%s)"
                    , nextColor.toString(), colorAttr.getName(), injectingPoint.getPath()), ex);
        }
        
        return colorAttr;
    }

    private void removeDataSourceFromGraph(DataSource removedNode) 
    {
        RRDef def = dataSources.remove(removedNode);
        if (def != null) {
            removeDataSourceFromGraphById(def.getParent(), ""+removedNode.getId());
        }
    }

    private void removeDataSourceFromGraphById(Node graphNode, String id) 
    {
        for (Node node : graphNode.getChildrens()) 
            if (node.getName().endsWith(id))
                tree.remove(node);
    }

    private void revalidateAttributeExpression(NodeAttribute attr, Node node) {
        if (!attr.isExpressionValid()) {
            try {
                attr.validateExpression();
            } catch (Exception ex) {
                logger.warn(String.format(
                        "Error validating expression for attribute (%s)" + " of node (%s)"
                        , attr.getName(), node.getPath()));
            }
        }
    }
    
    private void sync() throws Exception
    {
        if (lock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            try
            {
                //scan subtree for existing RRGraphNode for dataSources
                tree.scanSubtree(this, new ExistingDataSourcesScanner(), null);
                //scan subtree from starting point to find new data sources and create RRGraphNode
                tree.scanSubtree(startingPoint, new NewDataSourcesScanner(), null, Status.STARTED);
            }
            finally
            {
                lock.unlock();
            }
            startingPoint.addListener(startingPointListener);
        }
        else
        {
            throw new Exception(String.format("Error locking graphic manager (%s)", getPath()));
        }
    }
    
    private class ExistingDataSourcesScanner implements ScannedNodeHandler
    {
        public ScanOperation nodeScanned(Node node) 
        {
            if (node instanceof TemplateEntry)
                return ScanOperation.SKIP_NODE;
            if (node instanceof RRDef)
            {
                RRDef def = (RRDef) node;
                RRDataSource rrds = def.getDataSource();
                if (rrds==null || rrds.getDataSource()==null)
                {
                    String id = def.getName().substring(def.getName().lastIndexOf('_')+1);
                    removeDataSourceFromGraphById(def.getParent(), id);
                }
                else
                    dataSources.put(def.getDataSource().getDataSource(), def);
            }
            return ScanOperation.CONTINUE;
        }
    }

    private class NewDataSourcesScanner implements ScannedNodeHandler
    {
        public ScanOperation nodeScanned(Node node) 
        {
            if (node instanceof TemplateEntry)
                return ScanOperation.SKIP_NODE;

            if (!(node instanceof DataSource) || dataSources.containsKey(node))
                return ScanOperation.CONTINUE;

            addDataSourceToGraph((DataSource)node);

            return ScanOperation.CONTINUE;
        }
    }
    
    private class Tuner implements NodeTuner
    {
        private final DataSource dataSource;
        private final RRDataSource rrds;

        public Tuner(DataSource dataSource, RRDataSource rrds) 
        {
            this.dataSource = dataSource;
            this.rrds = rrds;
        }

        public Node cloneNode(Node sourceNode) 
        {
            if (sourceNode instanceof GroupNode)
            {
                String groupName = getGroupName(sourceNode);
                ContainerNode clone = new ContainerNode(groupName);
                return clone;
            }
            else
                return null;
        }

        public void tuneNode(Node sourceNode, Node sourceClone) 
        {
            if (sourceNode instanceof RRGraphNode)
            {
                String groupName = getGroupName(sourceNode);
                sourceClone.setName(groupName);
                sourceClone.removeNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
            }
            else if (!(sourceNode instanceof GroupNode))
            {
                //tunig nodes inside RRGraphNode
                sourceClone.setName(sourceNode.getName()+"_"+dataSource.getId());
            }
            
            //searching for attributes that referenced to the GraphNode childs
            if (sourceNode.getParent() instanceof RRGraphNode)
            {
                Collection<NodeAttribute> attrs = sourceNode.getNodeAttributes();
                if (attrs!=null)
                    for (NodeAttribute attr: attrs)
                        if (NodeReferenceValueHandlerFactory.TYPE.equals(attr.getValueHandlerType()))
                        {
                            Node value = attr.getRealValue();
                            if (value!=null && value.getParent()==sourceNode.getParent())
                            {
                                NodeAttribute clonedAttribute = 
                                        sourceClone.getNodeAttribute(attr.getName());
                                String path = clonedAttribute.getRawValue();
                                if (path.endsWith("\""))
                                    clonedAttribute.setRawValue(
                                            path.substring(0, path.length()-1)
                                            + "_"+dataSource.getId()+"\"");
                                else
                                    clonedAttribute.setRawValue(path+"_"+dataSource.getId());
                            }
                        }
            }
        }
        
        public void finishTuning(Node nodeClone) 
        {
            if (nodeClone instanceof RRDef)
            {
                NodeAttribute rrdAttr = 
                        nodeClone.getNodeAttribute(RRDef.DATASOURCE_ATTRIBUTE);
                try {
                    rrdAttr.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
                    rrdAttr.setValue(rrds.getPath());
                    rrdAttr.save();
                } catch (Exception e) 
                {
                    logger.error(
                        String.format(
                            "Error configuring RRDef node (%s) for dataSource (%s)"
                            , nodeClone.getPath(), rrds.getPath())
                        , e);
                }
                dataSources.put(dataSource, (RRDef)nodeClone);
            }
        }
        
        private String getGroupName(Node sourceNode)
        {
            NodeAttribute groupingExpression = sourceNode.getNodeAttribute(
                    GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
            if (groupingExpression==null)
                throw new NodeError(String.format(
                        "Error creating graph for dataSource (%s). " +
                        "Node (%s) must contains (%s) attribute",
                        dataSource.getPath(), sourceNode.getPath()
                        , GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));

            String groupName = groupingExpression.getValue();
            if (groupName==null)
                throw new NodeError(String.format(
                        "Error creating graph for dataSource (%s). Error in the node (%s). " +
                        "The value of the attribute (%s) is NULL"
                        , dataSource.getPath(), sourceNode.getPath()
                        , GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));
            
            return groupName;
        }
    }
    
    private class StartingPointListener extends NodeListenerAdapter
    {
        public StartingPointListener() 
        {
            super(true);
        }

        @Override
        public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus) 
        {
            if (   getStatus()==Status.STARTED 
                && node instanceof DataSource 
                && newStatus==Status.STARTED)
            {
                lock.lock();
                try{
                    if (!dataSources.containsKey(node))
                        addDataSourceToGraph((DataSource)node);
                }finally{
                    lock.unlock();
                }
            }
        }

        @Override
        public void dependendNodeAdded(Node node, Node dependentNode) 
        {
            if (   getStatus()==Status.STARTED 
                && node instanceof DataSource 
                && node.getStatus()==Status.STARTED
                && dependentNode instanceof RRDataSource)
            {
                lock.lock();
                try{
                    if (!dataSources.containsKey(node))
                        addDataSourceToGraph((DataSource)node);
                }finally{
                    lock.unlock();
                }
            }
        }

        @Override
        public void nodeRemoved(Node removedNode)
        {
            if (getStatus()!=Status.STARTED || !(removedNode instanceof DataSource))
                return;

            lock.lock();
            try {
                removeDataSourceFromGraph((DataSource)removedNode);
            }finally{
                lock.unlock();
            }
        }
    }
}
