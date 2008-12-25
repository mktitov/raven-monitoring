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
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.raven.conf.Configurator;
import org.raven.rrd.data.DBPool;
import org.raven.rrd.data.RRDNode;
import org.raven.statdb.impl.AbstractStatisticsDatabase;
import org.raven.statdb.impl.StatisticsDefinitionNode;

/**
 *
 * @author Mikhail Titov
 */
public class RrdStatisticsDatabaseNode extends AbstractStatisticsDatabase
{
	public static final String DATASOURCE_NAME="datasource";

	private DatabaseTemplatesNode databaseTemplatesNode;
	private File dbRoot;
	private RrdDbPool pool;

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
		File dbFile = new File(
				dbRoot.getAbsolutePath()+File.separator+key+File.separator+statisticsName+".jrb");
		if (!dbFile.exists())
			createDbFile(dbFile, statisticsName);
		RrdDb db = pool.requestRrdDb(dbFile.getAbsolutePath());
		try
		{
			db.createSample(time).setValue(0, value).update();
		}
		finally
		{
			pool.release(db);
		}
	}

	private void createDbFile(File dbFile, String statisticsName) throws Exception
	{
		StatisticsDefinitionNode statDef =
				(StatisticsDefinitionNode) statisticsDefinitions.getChildren(statisticsName);
		String statType = statDef.getType();
		RRDNode template = (RRDNode) databaseTemplatesNode.getChildren(statType);
		RrdDb db = template.createDatabase(dbFile.getAbsolutePath());
		if (db==null)
			throw new Exception("Error creating database (%s) from template (%s)");
		db.close();
	}

	@Override
	protected boolean isStatisticsDefenitionValid(StatisticsDefinitionNode statDef)
	{
		RRDNode template = (RRDNode) databaseTemplatesNode.getChildren(statDef.getType());
		
		if (template==null)
		{
			error("Invalid statistics (%s). Statistics does not have the database template");
			return false;
		}

		boolean hasDatasources = false;
		boolean hasArchived = false;


		return true;
	}
}
