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

package org.raven.statdb.rrd;

import java.io.File;
import java.util.Collection;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.statdb.impl.AbstractStatisticsDatabase;
import org.raven.statdb.impl.StatisticsDefinitionNode;
import org.raven.tree.Node;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RrdStatisticsDatabaseNode extends AbstractStatisticsDatabase
{
	public static final String DATASOURCE_NAME="datasource";

	@Parameter(defaultValue="now")
	@NotNull
	private String startTime;

	@Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	@NotNull
	private RrdUpdateQueueNode updateQueue;

	private DatabaseTemplatesNode databaseTemplatesNode;
	private File dbRoot;
	private RrdDbPool pool;

	public DatabaseTemplatesNode getDatabaseTemplatesNode()
	{
		return databaseTemplatesNode;
	}

	public String getStartTime()
	{
		return startTime;
	}

	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}

	public RrdUpdateQueueNode getUpdateQueue()
	{
		return updateQueue;
	}

	public void setUpdateQueue(RrdUpdateQueueNode updateQueue)
	{
		this.updateQueue = updateQueue;
	}

	@Override
	protected void initConfigurationNodes()
	{
		super.initConfigurationNodes();

		databaseTemplatesNode = (DatabaseTemplatesNode) getChildren(DatabaseTemplatesNode.NAME);
		if (databaseTemplatesNode==null)
		{
			databaseTemplatesNode = new DatabaseTemplatesNode();
			databaseTemplatesNode.setParent(this);
			databaseTemplatesNode.save();
			addChildren(databaseTemplatesNode);
			databaseTemplatesNode.init();
			databaseTemplatesNode.start();
		}
	}

	@Override
	protected void doStart() throws Exception
	{
		super.doStart();

		String dbsRoot = configurator.getConfig().getStringProperty(
				Configurator.RRD_STAT_DATABASES_PATH, null);
		if (dbsRoot==null)
			throw new Exception(String.format(
					"The parameter (%s) must be defined in the raven-monitoring configuration " +
					"file before using this node functionality"
					, Configurator.RRD_STAT_DATABASES_PATH));
		dbRoot = new File(dbsRoot+File.separator+getId());
		if (!dbRoot.exists())
		{
			if (!dbRoot.mkdir())
				throw new Exception(String.format(
						"Error creating directory (%s) for statistics database"
						, dbRoot.getAbsolutePath()));
		}
		else if (!dbRoot.isDirectory())
			throw new Exception(String.format(
					"The file (%s) must be a directory", dbRoot.getAbsolutePath()));

		pool = RrdDbPool.getInstance();
	}

	public void saveStatisticsValue(String key, String statisticsName, double value, long time)
			throws Exception
	{
		String dbFileDir = dbRoot.getAbsolutePath()+File.separator+key;
		File dbFile = new File(dbFileDir+File.separator+statisticsName+".jrb");

		if (!dbFile.exists())
			createDbFile(dbFile, statisticsName);

		updateQueue.pushUpdateRequest(new RrdUpdateRequest(dbFile.getAbsolutePath(), time, value));
//		RrdDb db = pool.requestRrdDb(dbFile.getAbsolutePath());
//		try
//		{
//			db.createSample(time).setValue(0, value).update();
//		}
//		finally
//		{
//			pool.release(db);
//		}
	}

	private void createDbFile(File dbFile, String statisticsName)
			throws Exception
	{
		File dbFileDir = dbFile.getParentFile();
		if (!dbFileDir.exists())
		{
			if (!dbFileDir.mkdirs())
				throw new Exception(String.format(
						"Error creating directory (%s)", dbFileDir.getAbsolutePath()));
		}
		StatisticsDefinitionNode statDef =
				(StatisticsDefinitionNode) statisticsDefinitions.getChildren(statisticsName);
		String statType = statDef.getType();
		RrdDatabaseDefNode template =
				(RrdDatabaseDefNode)databaseTemplatesNode.getChildren(statType);
		RrdDb db = template.createDatabase(dbFile.getAbsolutePath());
		db.close();
	}

	@Override
	protected boolean isStatisticsDefenitionValid(StatisticsDefinitionNode statDef)
	{
		RrdDatabaseDefNode template =
				(RrdDatabaseDefNode) databaseTemplatesNode.getChildren(statDef.getType());
		
		if (template==null)
		{
			error(String.format(
					"Invalid statistics (%s). Statistics does not have the database template"
					, statDef.getName()));
			return false;
		}

		if (template.getStatus()!=Status.STARTED)
		{
			error(String.format("Invalid statistics (%s). Database template (%s) not started"
					, statDef.getName(), template.getPath()));
			return false;
		}

		boolean hasDatasources = false;
		boolean hasArchives = false;

		Collection<Node> childs = template.getChildrens();
		if (childs!=null && childs.size()>0)
		{
			for (Node child: childs)
			{
				if (child.getStatus()!=Status.STARTED)
					continue;
				if (child instanceof RrdArchiveDefNode)
					hasArchives = true;
				else if (   child instanceof RrdDatasourceDefNode
						 && DATASOURCE_NAME.equals(child.getName()))
				{
					hasDatasources = true;
				}
			}
		}
		if (!hasArchives)
		{
			error(String.format(
					"Invalid statistics (%s). Database template (%s) must have at least " +
					"one archive"
					, statDef.getName(), template.getPath()));
			return false;
		}
		if (!hasDatasources)
		{
			error(String.format(
					"Invalid statistics (%s). Database template (%s) must have exactly " +
					"one datasource with name (%s)"
					, statDef.getName(), template.getPath(), DATASOURCE_NAME));
			return false;

		}

		return true;
	}
}
