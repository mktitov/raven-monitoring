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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jrobin.core.FetchData;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.Util;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.impl.AbstractStatisticsDatabase;
import org.raven.statdb.impl.KeyValuesImpl;
import org.raven.statdb.impl.QueryResultImpl;
import org.raven.statdb.impl.StatisticsDefinitionNode;
import org.raven.statdb.impl.StatisticsValuesImpl;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryExecutionException;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.SelectMode;
import org.raven.statdb.query.StatisticsValues;
import org.raven.tree.Node;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.RegexpFileFilter;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class RrdStatisticsDatabaseNode extends AbstractStatisticsDatabase
{
    public static final String DATABASE_FILE_EXTENSION = ".jrb";
	public static final String DATASOURCE_NAME="datasource";
	public static final String REGEXP_KEY_EXPRESSION = "@r ";

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
		File dbFile = formStatisticsDbFile(key, statisticsName);

		if (!dbFile.exists())
			createDbFile(dbFile, statisticsName);

		updateQueue.pushUpdateRequest(new RrdUpdateRequest(dbFile.getAbsolutePath(), time, value));
	}

	public QueryResult executeQuery(Query query) throws QueryExecutionException
	{
		try
		{
			Collection<KeyValues> keys = findKeys(dbRoot, query.getFromClause());
			if (   query.getSelectClause().getSelectMode() == SelectMode.SELECT_KEYS_AND_DATA
				&& keys.size()>0)
			{
				fetchStatisticsValues(query, keys);
			}

			return new QueryResultImpl(keys);
		}
		catch (Exception e)
		{
			String message = "Error executing query. "+e.getMessage();
			error(message, e);
			throw new QueryExecutionException(message, e);
		}
	}

    private void fetchStatisticsValues(Query query, Collection<KeyValues> keys) throws Exception
    {
        String[] statNames = query.getStatisticsNames();
        long[] timePeriod =
                RrdDatabaseDefNode.getTimePeriod(query.getStartTime(), query.getEndTime());
        long step = query.getStep();
        for (KeyValues keyValues: keys)
        {
            for (String statName: statNames)
            {
                File dbFile = formStatisticsDbFile(keyValues.getKey(), statName);
                if (dbFile.exists())
                {
                    RrdDb db = pool.requestRrdDb(dbFile.getAbsolutePath());
                    FetchRequest request = db.createFetchRequest(
                            "LAST", timePeriod[0], timePeriod[1], step);
                    FetchData fData = request.fetchData();
                    StatisticsValues values = new StatisticsValuesImpl(
                            statName, fData.getStep(), fData.getTimestamps(), fData.getValues(0));
                    ((KeyValuesImpl)keyValues).addStatisticsValues(values);
                }
            }
        }
    }

	private Collection<KeyValues> findKeys(File path, FromClause fromClause) throws Exception
	{
		String keyExpression = fromClause.getKeyExpression();
		
		if (keyExpression==null || keyExpression.trim().length()==0)
			throw new Exception("Key expression can not be empty");

		String[] elements = keyExpression.split(StatisticsDatabase.KEY_DELIMITER);
		if (elements==null || elements.length<=1)
			return Collections.EMPTY_LIST;
		
		FileFilter[] filters = new FileFilter[elements.length];
		for (int i=0; i<elements.length; ++i)
			if (elements[i].startsWith(REGEXP_KEY_EXPRESSION))
				filters[i] = new RegexpFileFilter(elements[i].substring(3));

		List<KeyValues> keys = new ArrayList<KeyValues>(100);

		extractKeys(KEY_DELIMITER, elements, filters, 1, path, keys);

		return keys.size()==0? Collections.EMPTY_LIST : keys;
	}
	
	private void extractKeys(
			String key, String[] elements, FileFilter[] filters, int elementPos, File path
			, List<KeyValues> keys)
	{
		File[] files = null;
		if (filters[elementPos]==null)
		{
			File file = new File(path.getAbsolutePath()+File.separator+elements[elementPos]);
			if (file.exists())
				files = new File[]{file};
		}
		else
			files = path.listFiles(filters[elementPos]);

		if (files==null || files.length==0)
			return;

		if ((elementPos+1)<elements.length)
			for (File file: files)
			{
				String newKey = key + file.getName() + KEY_DELIMITER;
				extractKeys(newKey, elements, filters, elementPos+1, file, keys);
			}
		else
			for (File file: files)
				keys.add(new KeyValuesImpl(key+file.getName()+KEY_DELIMITER));
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

    private File formStatisticsDbFile(String key, String statisticsName)
    {
		String dbFileDir = dbRoot.getAbsolutePath()+File.separator+key;
        return new File(dbFileDir+File.separator+statisticsName+DATABASE_FILE_EXTENSION);
    }

    private static double[] realignData(
            long[] ts, long queryStep, long[] dataTs, long dataStep, double[] data)
    {
        double[] result = new double[ts.length];

        int j=0; 
        long qs = queryStep-1;
        long ds = dataStep-1;
        for (int i=0; i<result.length; ++i)
        {
            while (between(dataTs[j], ts[i]-qs, ts[i]) || between(dataTs[j]-ds, ts[i]-qs, ts[i]))
            {
                ++j;
            }
            if (dataTs[j-1]>ts[i])
                --j;
        }

        return result;
    }

    private static boolean between(long v, long lowerBound, long upperBound)
    {
        return v>=lowerBound && v<=upperBound;
    }
}
