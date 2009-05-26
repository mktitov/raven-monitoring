/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.statdb.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.script.Bindings;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.QueryExecutionException;
import org.raven.statdb.query.QueryResult;
import org.raven.template.GroupsOrganazier;
import org.raven.template.impl.GroupsOrganizerNodeTuner;
import org.raven.template.impl.TemplateEntry;
import org.raven.template.impl.TemplateNode;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.expr.impl.BindingSupportImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SdbQueryNodeGeneratorNode extends BaseNode implements Schedulable
{
    public static final String GROUPNAME_BINDING = "groupName";
    public static final String GROUPLEVEL_BINDING = "groupLevel";
    public static final String KEY_BINDING = "key";
    public static final String LASTKEYELEMENT_BINDING = "lastKeyElement";
    
    public static final String GROUP_WITHOUT_NAME = "GROUP_WITHOUT_NAME";
    public static final int LOCK_TIMEOUT = 500;
    public static final String TEMPLATE_NODE_NAME = "Template";

    @Service
    private static GroupsOrganazier groupsOrganazier;
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @NotNull
    private StatisticsDatabase statisticsDatabase;

    @Parameter
    private String keyMasks;

    @Parameter(defaultValue="@r .*")
    @NotNull
    private String defaultKeyMask;

    @Parameter(defaultValue="/")
    @NotNull
    private String startFromKey;

    @Parameter
    @NotNull
    private String gropNames;

    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull
    private Scheduler scheduler;

    @Parameter(readOnly=true)
    private long sdbRequestsCount;

    private String[] groups;
    private String[] masks;
    private Lock updateLock;
    private BindingSupportImpl bindingSupport;

    private SdbQueryNodeGeneratorTemplateNode queryNodeGeneratorTemplate;

    public SdbQueryNodeGeneratorNode()
    {
        setStartAfterChildrens(true);
    }

    @Override
    protected void initFields()
    {
        super.initFields();

        updateLock = new ReentrantLock();
        sdbRequestsCount = 0;
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();

        generateTemplateNode();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        generateTemplateNode();

        groups = RavenUtils.split(gropNames);
        masks = RavenUtils.split(keyMasks);

        deletePreviousGroups();
        String _startFromKey = startFromKey;
        createGroup(_startFromKey, this);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();

        deletePreviousGroups();
    }
    
    public void executeScheduledJob()
    {
        if (getStatus().equals(Status.STARTED))
        {
            try
            {
                if (updateLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS))
                {
                    try
                    {
                        deletePreviousGroups();
                        createGroup(startFromKey, this);
                    }
                    finally
                    {
                        updateLock.unlock();
                    }
                }
                else
                {
                    error("Error acquiring update lock for clean operation");
                }
            }
            catch(InterruptedException e)
            {
                error("Error acquiring update lock", e);
            }
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
        if (!isTemplate())
        {
            bindings.put(TemplateNode.TEMPLATE_EXPRESSION_BINDING, this);
        }
    }

    public long getSdbRequestsCount()
    {
        return sdbRequestsCount;
    }
    
    public Scheduler getScheduler()
    {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public String getDefaultKeyMask()
    {
        return defaultKeyMask;
    }

    public void setDefaultKeyMask(String defaultKeyMask)
    {
        this.defaultKeyMask = defaultKeyMask;
    }

    public TemplateEntry getQueryNodeGeneratorTemplate()
    {
        return queryNodeGeneratorTemplate;
    }

    public String getGropNames()
    {
        return gropNames;
    }

    public void setGropNames(String gropNames)
    {
        this.gropNames = gropNames;
    }

    public String getKeyMasks()
    {
        return keyMasks;
    }

    public void setKeyMasks(String keyMasks)
    {
        this.keyMasks = keyMasks;
    }

    public String getStartFromKey()
    {
        return startFromKey;
    }

    public void setStartFromKey(String startFromKey)
    {
        this.startFromKey = startFromKey;
    }

    public StatisticsDatabase getStatisticsDatabase()
    {
        return statisticsDatabase;
    }

    public void setStatisticsDatabase(StatisticsDatabase statisticsDatabase)
    {
        this.statisticsDatabase = statisticsDatabase;
    }

    public void addChildrensFor(SdbQueryNodeGeneratorGroupNode group)
    {
        if (!getStatus().equals(Status.STARTED))
            return;
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Adding childrens to group (%s)", group.getPath()));
        try
        {
            if (updateLock.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS))
            {
                try
                {
                    if (group.getStatus().equals(Status.REMOVED))
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug(String.format(
                                    "Skipped addind nodes operation to the group node (%s) " +
                                    "because of clean operation."
                                    , group.getPath()));
                        return;
                    }
                    if (group.isChildsInitialized())
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug(String.format(
                                    "Skipped addind nodes operation to the group node (%s) " +
                                    "because of child nodes already added by other thread."
                                    , group.getPath()));
                        return;
                    }
                    ++sdbRequestsCount;
                    Collection<String> keys = getKeysForGroup(group);
                    if (keys==null)
                        return;

                    int level = getGroupNodeLevel(group);
                    String groupName = getGroupNodeName(level);
                    for (String key: keys)
                    {
                        String childsKeyExpression = getChildsKeyExpression(key, level);
                        String[] keyElements = RavenUtils.split(
                                key, StatisticsDatabase.KEY_DELIMITER);
                        String lastKeyElement = keyElements[keyElements.length-1];

                        Tuner tuner = new Tuner(
                                level, groupName, childsKeyExpression, lastKeyElement);

                        bindingSupport.put(GROUPNAME_BINDING, groupName);
                        bindingSupport.put(GROUPLEVEL_BINDING, level);
                        bindingSupport.put(LASTKEYELEMENT_BINDING, lastKeyElement);
                        bindingSupport.put(KEY_BINDING, key);
                        try
                        {
                            groupsOrganazier.organize(
                                    group, queryNodeGeneratorTemplate, tuner, null, true);
                        }
                        finally
                        {
                            bindingSupport.reset();
                        }
                    }
                }
                finally
                {
                    updateLock.unlock();
                }
            }
            else
                error(String.format(
                        "Error acquiring update lock for adding childrens to the (%s) group node"
                        , group.getPath()));
        }
        catch (InterruptedException e)
        {
                error(
                    String.format(
                        "Error acquiring update lock for adding childrens to the (%s) group node"
                        , group.getPath())
                    , e);
        }
    }

    private void createGroup(String statisticsDatabaseKey, Node parentNode)
    {
        int level = getGroupNodeLevel(parentNode);
        String groupName = getGroupNodeName(level);
        String childsKeyExpression = getChildsKeyExpression(statisticsDatabaseKey, level);
        SdbQueryNodeGeneratorGroupNode group = new SdbQueryNodeGeneratorGroupNode();
        group.setName(groupName);
        group.setNodeGenerator(this);
        parentNode.addAndSaveChildren(group);
        group.setChildsKeyExpression(childsKeyExpression);
        group.start();
    }

    private int getGroupNodeLevel(Node parentNode)
    {
        int level = 0;
        Node currentNode = parentNode;
        while (currentNode!=this)
        {
            if (currentNode instanceof SdbQueryNodeGeneratorGroupNode)
                ++level;
            currentNode = currentNode.getParent();
        }
        return level;
    }

    private String getGroupNodeName(int level)
    {
        return level>=groups.length? GROUP_WITHOUT_NAME : groups[level];
    }

    private String getChildsKeyExpression(String key, int level)
    {
        String keyMask = masks==null || level>=masks.length? defaultKeyMask : masks[level];
        return key+keyMask;
    }

    private void deletePreviousGroups()
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null && !childs.isEmpty())
        {
            for (Node child: childs)
                if (child instanceof SdbQueryNodeGeneratorGroupNode)
                {
                    tree.remove(child);
                    break;
                }
        }
    }

    private void generateTemplateNode()
    {
        queryNodeGeneratorTemplate =
                (SdbQueryNodeGeneratorTemplateNode)getChildren(TEMPLATE_NODE_NAME);
        if (queryNodeGeneratorTemplate==null)
        {
            queryNodeGeneratorTemplate = new SdbQueryNodeGeneratorTemplateNode();
            queryNodeGeneratorTemplate.setName(TEMPLATE_NODE_NAME);
            addAndSaveChildren(queryNodeGeneratorTemplate);
            queryNodeGeneratorTemplate.start();
        }
    }

    private Collection<String> getKeysForGroup(SdbQueryNodeGeneratorGroupNode group)
    {
        StatisticsDatabase _database = statisticsDatabase;
        if (!_database.getStatus().equals(Status.STARTED))
        {
            error(String.format(
                    "Can not execute query on statistics database (%s). Database not STARTED"
                    , _database.getPath()));
            return null;
        }

        SelectKeysQuery query = new SelectKeysQuery(group.getChildsKeyExpression());
        try
        {
            QueryResult queryResult = statisticsDatabase.executeQuery(query);
            
            Collection<KeyValues> keyValues = queryResult.getKeyValues();
            if (keyValues.isEmpty())
                return null;
            
            Collection<String> keys = new ArrayList<String>(keyValues.size());
            for (KeyValues values: keyValues)
                keys.add(values.getKey());

            return keys;
        }
        catch (QueryExecutionException ex)
        {
            error(String.format(
                    "Error executing query for generating child nodes for group node (%s). " +
                    "Database: (%s), keyExpression (%s)"
                    , group.getPath(), _database.getPath(), group.getChildsKeyExpression()));
            return null;
        }
    }

    private class Tuner extends GroupsOrganizerNodeTuner
    {
        private final int groupNodeLevel;
        private final String groupName;
        private final String childsKeyExpression;
        private final String lastKeyElement;

        public Tuner(
                int groupNodeLevel, String groupName, String childsKeyExpression
                , String lastKeyElement)
        {
            this.groupNodeLevel = groupNodeLevel;
            this.groupName = groupName;
            this.childsKeyExpression = childsKeyExpression;
            this.lastKeyElement = lastKeyElement;
        }

        @Override
        public void tuneNode(Node sourceNode, Node sourceClone)
        {
            super.tuneNode(sourceNode, sourceClone);

            if (sourceClone instanceof SdbQueryNodeGeneratorGroupNode)
            {
                SdbQueryNodeGeneratorGroupNode group = (SdbQueryNodeGeneratorGroupNode) sourceClone;
                group.setNodeGenerator(SdbQueryNodeGeneratorNode.this);
                if (   sourceClone.getName()==null
                    || sourceClone.getName().equals(sourceNode.getName()))
                {
                    group.setName(groupName);
                    group.setChildsKeyExpression(childsKeyExpression);
                }
            }
        }

        @Override
        protected void formBindings(Bindings bindings)
        {
            super.formBindings(bindings);
            SdbQueryNodeGeneratorNode.this.formExpressionBindings(bindings);
            bindingSupport.addTo(bindings);
        }
    }
}
