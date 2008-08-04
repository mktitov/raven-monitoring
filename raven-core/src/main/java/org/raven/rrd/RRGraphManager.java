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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataSource;
import org.raven.rrd.data.RRDataSource;
import org.raven.rrd.graph.RRDef;
import org.raven.rrd.graph.RRGraphNode;
import org.raven.template.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeTuner;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.ContainerNode;
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
    public final static long LOCK_TIMEOUT = 500;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @Description("The node from which graph manager take a control on dataSources")
    @NotNull
    private Node startingPoint;
    
    private RRGraphManagerTemplate template;
    private Lock lock;
    private HashMap<DataSource, RRDef> dataSources;
    private Map<String, Object> expressionContext;

    @Override
    protected void initFields()
    {
        super.initFields();
        lock = new ReentrantLock();
        dataSources = new HashMap<DataSource, RRDef>();
        expressionContext = new HashMap<String, Object>();
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

    public Node getStartingPoint()
    {
        return startingPoint;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        
        bindings.putAll(expressionContext);
    }

    private void addDataSourceToGraph(DataSource dataSource, RRDataSource rrds) 
    {
        expressionContext.put("dataSource", dataSource);
        expressionContext.put("rrDataSource", rrds);
        
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
            Collection<Node> childs = gTemplate.getChildrens();
            if (childs!=null)
                for (Node node: childs)
                    tree.copy(node, injectingPoint, null, tuner, true, false);
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

            RRDataSource rrds = null;
            Collection<Node> nodeDeps = node.getDependentNodes();
            if (nodeDeps==null)
                for (Node dep: nodeDeps)
                    if (dep instanceof RRDataSource)
                    {
                        rrds = (RRDataSource) dep;
                        break;
                    }

            if (rrds==null)
                return ScanOperation.CONTINUE;

            addDataSourceToGraph((DataSource)node, rrds);

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
            }
            else if (!(sourceNode instanceof GroupNode))
            {
                //tunig nodes inside RRGraphNode
                sourceClone.setName(sourceNode.getName()+"_"+dataSource.getId());
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
    
}
