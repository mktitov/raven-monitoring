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
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.RavenUtils;
import org.raven.annotations.Parameter;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.QueryExecutionException;
import org.raven.statdb.query.QueryResult;
import org.raven.template.GroupsOrganazier;
import org.raven.template.impl.GroupsOrganizerNodeTuner;
import org.raven.template.impl.TemplateEntry;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class QueryNodeGeneratorNode extends BaseNode
{
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

    private String[] groups;
    private String[] masks;

    private TemplateEntry queryNodeGeneratorTemplate;

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
        String _startFromKey =
                StatisticsDatabase.KEY_DELIMITER.equals(startFromKey)? "" : startFromKey;
        createGroup(_startFromKey, this);
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();

        deletePreviousGroups();
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

    public void addChildrensFor(QueryNodeGeneratorGroupNode group)
    {
        Collection<String> keys = getKeysForGroup(group);
        if (keys==null)
            return;

        
    }

    private void createGroup(String statisticsDatabaseKey, Node parentNode)
    {
        int level = 0;
        Node currentNode = parentNode;
        while (currentNode!=this)
        {
            if (currentNode instanceof QueryNodeGeneratorGroupNode)
                ++level;
            currentNode = currentNode.getParent();
        }
        String groupName = level>=groups.length? "GROUP_WITHOUT_NAME" : groups[level];
        String keyMask = masks==null || level>=masks.length? defaultKeyMask : masks[level];
        QueryNodeGeneratorGroupNode group = new QueryNodeGeneratorGroupNode();
        group.setName(groupName);
        group.setNodeGenerator(this);
        parentNode.addAndSaveChildren(group);
        group.setChildsKeyExpression(
                statisticsDatabaseKey+StatisticsDatabase.KEY_DELIMITER+keyMask);
        group.start();
    }

    private void tuneNode(QueryNodeGeneratorGroupNode group)
    {
        
    }

    private void deletePreviousGroups()
    {
        Collection<Node> childs = getChildrens();
        if (childs!=null && childs.isEmpty())
        {
            for (Node child: childs)
                if (child instanceof QueryNodeGeneratorGroupNode)
                {
                    tree.remove(child);
                    break;
                }
        }
    }

    private void generateTemplateNode()
    {
        queryNodeGeneratorTemplate = (TemplateEntry) getChildren(TEMPLATE_NODE_NAME);
        if (queryNodeGeneratorTemplate==null)
        {
            queryNodeGeneratorTemplate = new TemplateEntry();
            queryNodeGeneratorTemplate.setName(TEMPLATE_NODE_NAME);
            addAndSaveChildren(queryNodeGeneratorTemplate);
            queryNodeGeneratorTemplate.start();
        }
    }

    private Collection<String> getKeysForGroup(QueryNodeGeneratorGroupNode group)
    {
        StatisticsDatabase _database = statisticsDatabase;
        if (!_database.getStatus().equals(Status.STARTED))
        {
            error(String.format(
                    "Can not execute query on statistics database (%s). Database not STARTED"
                    , _database.getPath()));
            return null;
        }

        SelectKeysQuery query =
                new SelectKeysQuery(group.getChildsKeyExpression(), statisticsDatabase);
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
        @Override
        public Node cloneNode(Node sourceNode)
        {
            Node newNode = super.cloneNode(sourceNode);
            newNode = newNode==null? sourceNode : newNode;
            if (newNode instanceof QueryNodeGeneratorGroupNode)
            {
                QueryNodeGeneratorGroupNode group = (QueryNodeGeneratorGroupNode) newNode;
                group.setNodeGenerator(QueryNodeGeneratorNode.this);
                group.set
            }
        }
    }
}
