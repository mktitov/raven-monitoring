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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import javax.script.SimpleBindings;
import org.jrobin.core.FetchData;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.Util;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.ds.Record;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCompiler;
import org.raven.expr.impl.GroovyExpressionCompiler;
import org.raven.rrd.DataSourceType;
import org.raven.statdb.Aggregation;
import org.raven.statdb.AggregationFunction;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.StatisticsRecord;
import org.raven.statdb.impl.AbstractStatisticsDatabase;
import org.raven.statdb.impl.AggregationCalculationUnit;
import org.raven.statdb.impl.ConstantAggregationCalculationUnit;
import org.raven.statdb.impl.ExpressionCalculationUnit;
import org.raven.statdb.impl.KeyValuesImpl;
import org.raven.statdb.impl.QueryResultImpl;
import org.raven.statdb.impl.StatisticsValuesImpl;
import org.raven.statdb.query.FromClause;
import org.raven.statdb.query.KeyValues;
import org.raven.statdb.query.OrderClause;
import org.raven.statdb.query.Query;
import org.raven.statdb.query.QueryExecutionException;
import org.raven.statdb.query.QueryResult;
import org.raven.statdb.query.QueryStatisticsName;
import org.raven.statdb.query.SelectClause;
import org.raven.statdb.query.SelectEntry;
import org.raven.statdb.query.SelectEntryCalculationUnit;
import org.raven.statdb.query.SelectMode;
import org.raven.statdb.query.StatisticsValues;
import org.raven.tree.Node;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.util.RegexpFileFilter;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class RrdStatisticsDatabaseNode extends AbstractStatisticsDatabase
{
    public static final String DATABASE_FILE_EXTENSION = ".jrb";
	public static final String DATASOURCE_NAME="datasource";
	public static final String REGEXP_KEY_EXPRESSION = "@r ";

    @Service
    private static ExpressionCompiler expressionCompiler;

	@Parameter(defaultValue="now")
	@NotNull
	private String startTime;

	@Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	@NotNull
	private RrdUpdateQueueNode updateQueue;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useLocalTime;

//	private DatabaseTemplatesNode databaseTemplatesNode;
	private File dbRoot;
	private RrdDbPool pool;

    public Boolean getUseLocalTime()
    {
        return useLocalTime;
    }

    public void setUseLocalTime(Boolean useLocalTime)
    {
        this.useLocalTime = useLocalTime;
    }

//	public DatabaseTemplatesNode getDatabaseTemplatesNode()
//	{
//		return databaseTemplatesNode;
//	}

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
	}

    @Override
    protected StatisticsRecord createStatisticsRecord(Record record) throws Exception
    {
        return new RrdStatisticsRecord(record, converter, this, useLocalTime);
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
			List<KeyValues> keys = findKeys(dbRoot, query.getFromClause());
            QueryResultImpl queryResult = new QueryResultImpl(keys);
			if (   query.getSelectMode() == SelectMode.SELECT_KEYS_AND_DATA
				&& keys.size()>0)
			{
				fetchStatisticsValues(query, queryResult);
                SelectClause selectClause = query.getSelectClause();
                if (selectClause!=null)
                {
                    SelectEntry[] selectEntries = selectClause.getSelectEntries();
                    if (selectEntries!=null)
                        calculateSelectEntriesValues(query, queryResult, selectEntries);
                }
                sortQueryResult(keys, query.getOrderClause());
			}

			return queryResult;
		}
		catch (Exception e)
		{
            String message = null;
            if (query instanceof Node)
                message = String.format(
                        "Error executing query (%s). %s", ((Node)query).getPath(), e.getMessage());
            else
                message = "Error executing query. "+e.getMessage();
			error(message, e);
			throw new QueryExecutionException(message, e);
		}
	}

    private void fetchStatisticsValues(Query query, QueryResultImpl queryResult) throws Exception
    {
        QueryStatisticsName[] statNames = query.getStatisticsNames();
        if (statNames==null || statNames.length==0)
            throw new Exception(String.format("Query must contains statistics names"));

        //calculating and normalizing start and end time
        long step = query.getStep();
        long[] timePeriod =
                RrdDatabaseDefNode.getTimePeriod(query.getStartTime(), query.getEndTime());
        timePeriod[0] = Util.normalize(timePeriod[0], step);
        timePeriod[1] = Util.normalize(timePeriod[1], step);
        //calculating timestamps
        long[] timestamps = new long[(int)((timePeriod[1]-timePeriod[0])/step)+1];
        for (int i=0; i<timestamps.length; ++i)
            timestamps[i] = timePeriod[0]+(step*i);

        queryResult.setStep(step);
        queryResult.setTimestamps(timestamps);

        String orderStat = query.getOrderClause()==null? null :
            query.getOrderClause().getStatisticName();

        for (KeyValues keyValues: queryResult.getKeyValues())
        {
            for (int i=0; i<statNames.length; ++i)
            {
                String statName = statNames[i].getName();
                File dbFile = formStatisticsDbFile(keyValues.getKey(), statName);
                if (dbFile.exists())
                {
                    RrdDb db = pool.requestRrdDb(dbFile.getAbsolutePath());
                    boolean integratedValue = !db.getDatasource(0).getDsType().equals(
                            DataSourceType.GAUGE.toString());
                    try
                    {
                        FetchRequest request = db.createFetchRequest(
                                statNames[i].getAggregationFunction().toString()
                                , timePeriod[0], timePeriod[1], step);
                        FetchData fData = request.fetchData();
                        double[] data = fData.getValues(0);
                        long[] dataTs = fData.getTimestamps();
//                        long lastTs = Util.normalize(db.getHeader().getLastUpdateTime(), step);
//                        double lastValue = db.getDatasource(0).getLastValue();
//                        int lastTsInd = (int) ((lastTs - timePeriod[0]) / step);
//                        if (lastTsInd>0 && lastTsInd<dataTs.length)
//                            data[lastTsInd]=lastValue;
                        if (fData.getStep()!=step || fData.getFirstTimestamp()!=timePeriod[0]
                            || data.length!=timestamps.length)
                        {
                            data = realignData(
                                    integratedValue, statNames[i].getAggregationFunction()
                                    , timestamps, step, dataTs, fData.getStep(), data);
                        }
                        StatisticsValues values = new StatisticsValuesImpl(statName, data);
                        ((KeyValuesImpl)keyValues).addStatisticsValues(values);
                        if (statName.equals(orderStat) && data.length>0)
                            ((KeyValuesImpl)keyValues).setWeight(data[0]);
                    }
                    finally
                    {
                        pool.release(db);
                    }
                }
            }
        }

        if (useLocalTime)
        {
            timestamps = queryResult.getTimestamps();
            for (int i=0; i<timestamps.length; ++i)
                timestamps[i] -= TimeZone.getDefault().getOffset(timestamps[i]*1000l);
        }
    }

    private void calculateSelectEntriesValues(
                Query query, QueryResultImpl queryResult, SelectEntry[] selectEntries)
            throws Exception
    {
        SelectEntryCalculationUnit[] units = new SelectEntryCalculationUnit[selectEntries.length];
        boolean hasAggregations = false;
        for (int i=0; i<selectEntries.length; ++i)
        {
            units[i] = AggregationCalculationUnit.checkAndCreate(selectEntries[i]);
            if (units[i]!=null)
                hasAggregations = true;
        }

        if (hasAggregations)
        {
            for (int i=0; i<units.length; ++i)
            {
                if (units[i]==null)
                {
                    Expression expression = expressionCompiler.compile(
                            selectEntries[i].getExpression(), GroovyExpressionCompiler.LANGUAGE);
                    units[i] = new ConstantAggregationCalculationUnit(
                            selectEntries[i].getName(), expression);
                }
            }
        }
        else
            for (int i=0; i<units.length; ++i)
            {
                Expression expression = expressionCompiler.compile(
                        selectEntries[i].getExpression(), GroovyExpressionCompiler.LANGUAGE);
                units[i] = new ExpressionCalculationUnit(
                        expression, selectEntries[i].getName(), queryResult.getValuesCount());
            }

        SimpleBindings bindings = new SimpleBindings();
        for (KeyValues keyValues: queryResult.getKeyValues())
        {
            KeyValuesImpl kValues = (KeyValuesImpl) keyValues;
            for (int i=0; i<queryResult.getValuesCount(); ++i)
            {
                bindings.clear();
                for (StatisticsValues values: keyValues.getStatisticsValues())
                    bindings.put(values.getStatisticsName(), values.getValues()[i]);
                for (SelectEntryCalculationUnit unit: units)
                    unit.calculate(bindings);
            }
            kValues.clear();
            for (SelectEntryCalculationUnit unit: units)
            {
                kValues.addStatisticsValues(unit.getStatisticsValues());
                unit.reset();
            }
        }

        if (hasAggregations)
            queryResult.compressTime();
    }

	private List<KeyValues> findKeys(File path, FromClause fromClause) throws Exception
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
                if (file.isDirectory())
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
        RecordSchema _schema = getRecordSchema();
        RecordSchemaField statField = RavenUtils.getRecordSchemaField(_schema, statisticsName);
        if (statField==null)
            throw new Exception(String.format(
                    "Record schema (%s) does not contains field (%s)"
                    , _schema.getName(), statisticsName));
        RrdDatabaseRecordFieldExtension fieldExt = statField.getFieldExtension(
                RrdDatabaseRecordFieldExtension.class, null);
        if (fieldExt==null)
            throw new Exception(String.format(
                    "The field (%s) of the record schema (%s) does not contains (%s) extension"
                    , statisticsName, _schema.getName()
                    , RrdDatabaseRecordFieldExtension.class.getSimpleName()));
        RrdDatabaseRecordExtension template = _schema.getRecordExtension(
                RrdDatabaseRecordExtension.class, fieldExt.getDatabaseTemplateName());
        if (template==null)
            throw new Exception(String.format(
                    "Record schema (%s) does not contains extension (%s) with name (%s)"
                    , _schema.getName()
                    , RrdDatabaseRecordExtension.class.getSimpleName()
                    , fieldExt.getDatabaseTemplateName()));
                    
		RrdDb db = template.createDatabase(dbFile.getAbsolutePath());
        
		db.close();
	}

    private File formStatisticsDbFile(String key, String statisticsName)
    {
		String dbFileDir = dbRoot.getAbsolutePath()+File.separator+key;
        return new File(dbFileDir+File.separator+statisticsName+DATABASE_FILE_EXTENSION);
    }

    static double[] realignData(
            boolean integratedValue, AggregationFunction aggType
            , long[] ts, long queryStep, long[] dataTs, long dataStep, double[] data)
    {
        double[] result = new double[ts.length];
        Arrays.fill(result, Double.NaN);

        int j=0; 
        long qs = queryStep-1;
        long ds = dataStep-1;
        Aggregation agg;
        double th, tl, val;
        for (int i=0; i<result.length; ++i)
        {
            agg = null;
            while (j<data.length
               && (between(dataTs[j], ts[i]-qs, ts[i]) || between(dataTs[j]-ds, ts[i]-qs, ts[i])))
            {
                th = dataTs[j]>ts[i]? ts[i] : dataTs[j];
                tl = dataTs[j]-ds<ts[i]-qs? ts[i]-queryStep : dataTs[j]-dataStep;
                val = !integratedValue? data[j] : data[j]/dataStep*(th-tl);
                
                if (agg==null)
                {
                    AggregationFunction func = integratedValue? AggregationFunction.SUM : aggType;
                    agg = func.createAggregation(0, val);

                }
                else
                    agg.aggregate(val);
                
                ++j;
            }
            if (j==0)
            {
                result[i] = Double.NaN;
            }
            else
            {
                if (dataTs[j-1]>ts[i])
                    --j;
                result[i] = agg!=null? agg.getValue() : Double.NaN;
            }
        }

        return result;
    }

    private static boolean between(long v, long lowerBound, long upperBound)
    {
        return v>=lowerBound && v<=upperBound;
    }

    private void sortQueryResult(
            List<KeyValues> keys, OrderClause orderClause)
    {
        if (orderClause==null)
            return;
        Collections.sort(keys, new QueryResultComparator(orderClause));
    }

    private class QueryResultComparator implements Comparator<KeyValues>
    {
        private boolean reverseOrder;

        public QueryResultComparator(OrderClause orderClause)
        {
            this.reverseOrder = orderClause.getReverseOrder();
        }

        public int compare(KeyValues o1, KeyValues o2)
        {
            if (reverseOrder)
                return Double.compare(
                        ((KeyValuesImpl)o2).getWeight(), ((KeyValuesImpl)o1).getWeight());
            else
                return Double.compare(
                        ((KeyValuesImpl)o1).getWeight(), ((KeyValuesImpl)o2).getWeight());
        }
    }
}
