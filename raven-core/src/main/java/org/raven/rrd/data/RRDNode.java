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

package org.raven.rrd.data;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jrobin.core.ArcDef;
import org.jrobin.core.Archive;
import org.jrobin.core.Datasource;
import org.jrobin.core.DsDef;
import org.jrobin.core.FetchData;
import org.jrobin.core.FetchRequest;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.RrdToolkit;
import org.jrobin.core.Sample;
import org.jrobin.core.Util;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.ds.ArchiveException;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.log.LogLevel;
import org.raven.rrd.ConsolidationFunction;
import org.raven.rrd.RRIoQueueNode;
import org.raven.table.DataArchiveTable;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.NodeShutdownError;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class RRDNode extends BaseNode implements DataConsumer, NodeListener
{
	public enum SampleUpdatePolicy {UPDATE_WHEN_READY, UPDATE_WHEN_TIME_EXPIRED};

	@Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
	@NotNull()
	private RRIoQueueNode ioQueue;
	
    @Parameter(defaultValue="300")
	@NotNull
    private Long step;
    
    @Parameter
    private String databaseFileName;
    
    @Parameter(defaultValue="false")
	@NotNull
    private Boolean backup;

	@Parameter(defaultValue="UPDATE_WHEN_READY")
	@NotNull
	private SampleUpdatePolicy sampleUpdatePolicy;

	@Parameter(defaultValue="now")
	@NotNull
	private String startTime;

	@Parameter(readOnly=true)
	private long writesCount;
    
    private AtomicBoolean databaseInitialized;
    private RrdDb db;
    private ReentrantReadWriteLock dbLock;
    private Map<String, String> newDsNames;
    private ReentrantLock newDsNamesLock;
    private Sample sample;
	private DataSample dataSample;
            
    public RRDNode()
    {
        setInitializeAfterChildrens(true);
    }
    
    @Override
    protected void initFields()
    {
        super.initFields();
        
        databaseInitialized = new AtomicBoolean(false);
        dbLock = new ReentrantReadWriteLock();
        newDsNames = null;
        newDsNamesLock = new ReentrantLock();
        sample = null;
        db = null;
		writesCount = 0l;
    }

    public Lock getReadLock()
    {
        return dbLock.readLock();
    }

    @Override
    protected void doStart() throws Exception
    {
        initDataBase();
    }

    @Override
    public synchronized void stop() throws NodeError
    {
        try
        {
            dbLock.writeLock().lock();
            try
            {
                if (databaseInitialized.get())
                {
                    databaseInitialized.set(false);
                    closeDatabase();
                }
                super.stop();
            }
            finally
            {
                dbLock.writeLock().unlock();
            }
        } catch (Exception e)
        {
            logger.error(String.format("Error stoping node (%s)", getPath()), e);
        }
    }

    public DataArchiveTable getArchivedData(DataSource dataSource, String fromDate, String toDate)
            throws ArchiveException
    {
        try {
            if (getStatus()!=Status.STARTED)
                throw new Exception("node must be in STARTED state.");
            Lock readLock = getReadLock();
            if (readLock.tryLock(5, TimeUnit.SECONDS)) 
            {
                try
                {
                    long[] timeRange = Util.getTimestamps(fromDate, toDate);
                    FetchRequest fetchRequest = db.createFetchRequest(
                            ConsolidationFunction.AVERAGE.name()
                            , timeRange[0], timeRange[1]);
                    fetchRequest.setFilter(dataSource.getName());
                    FetchData fetchData = fetchRequest.fetchData();
                    double[] values = fetchData.getValues(dataSource.getName());
                    long[] timestamps = fetchData.getTimestamps();
                    DataArchiveTable table = new DataArchiveTable();
                    for (int i=0; i<fetchData.getRowCount(); ++i)
						if (   timeRange[0]<(timestamps[i]+db.getHeader().getStep())
							&& timeRange[1]>=timestamps[i])
						{
							table.addData(Util.getDate(timestamps[i]), values[i]);
						}
                    return table;
                }
                finally
                {
                    readLock.unlock();
                }
            }
            else
                throw new Exception("Error getting read lock for database.");
        } catch (Exception ex)
        {
            throw new ArchiveException(
                    String.format("Error extracting data from round robin db (%s). %s"
                        , getPath(), ex.getMessage())
                    , ex);
        }
    }

    @Override
    public synchronized void shutdown() throws NodeShutdownError 
    {
        super.shutdown();
        
        dbLock.writeLock().lock();
        try
        {
            if (databaseInitialized.get())
            {
                databaseInitialized.set(false);
                closeDatabase();
            }
        }
        catch (Exception ex) 
        {
            throw new NodeShutdownError(String.format(
                    "Error shutdowning node (%s)", getPath())
                    , ex);
        } 
        finally
        {
            dbLock.writeLock().unlock();
        }
    }

    @Override
    public synchronized void remove() 
    {
        super.remove();
        
        if (databaseFileName!=null && databaseFileName.length()>0)
        {
            File dbFile = new File(databaseFileName);
            if (!dbFile.delete())
                logger.error(String.format(
                        "Error removing rrd database (%s) attached to the node (%s)"
                        , databaseFileName, getPath()));
        }
    }

    @Override
    public synchronized void addChildren(Node node)
    {
        super.addChildren(node);
        
        node.addListener(this);
        if (node instanceof RRDataSource)
            node.addDependentNode(this);
    }

    @Override
    public void removeChildren(Node node)
    {
        try
        {
            if (!ObjectUtils.in(getStatus(), Status.STARTED, Status.REMOVING))
                throw new Exception(
                        "Error removing RRDNode. The node must be started.");
            super.removeChildren(node);
            
            if (getStatus()==Status.REMOVING)
                return;
            if (node instanceof RRDataSource)
            {
                removeDataSource(node.getName(), true);
            } else if (node instanceof RRArchive)
            {
                RRArchive raa = (RRArchive) node;
                String conFun = converter.convert(
                        String.class, raa.getConsolidationFunction(), null);
                removeArchive(conFun, raa.getSteps(), true);
            }
        } catch (Exception e)
        {
            throw new NodeError(String.format(
                    "Error removing child node (%s) from the (%s) node", node.getPath(), getPath())
                    , e);
        }
    }
    
    public int getDataSourceCount()
    {
        int count=0;
        Collection<Node> childs = getChildrens();
        if (childs!=null)
            for (Node child: childs)
                if (child instanceof RRDataSource)
                    ++count;
        return count;
    }

    public Object refereshData(Collection<NodeAttribute> sessionAttributes) 
    {
        return null;
    }

    public void setData(DataSource dataSource, Object data)
    {
		if (getStatus()!=Status.STARTED)
			return;
		ioQueue.pushWriteRequest((RRDataSource)dataSource,data);
    }

	public void setDataFromQueue(RRDataSource dataSource, Object data)
	{
        try
        {
            long time = Util.getTime();
            dbLock.writeLock().lock();
            try
            {
                if (databaseInitialized.get())
                {
					if (dataSample==null)
						dataSample = new DataSample(this);
					DataSample.SetResult res =
							dataSample.checkAndSetValue((RRDataSource)dataSource, data);
					switch (res)
					{
						case INVALID_TIME: 
							warn(String.format(
									"The time for data (%s) for dataSource (%s) is less than " +
									"sample time (%s)."
									, data, dataSource.getPath(), dataSample.getSampleTime()));
							break;
						case TIME_EXPIRED:
							if (isDebugEnabled())
								debug("The time for current dataSample was expired. " +
										"Need to save sample data");
							saveSampleData();
							setDataFromQueue(dataSource, data);
							break;
						case VALUE_SETTED:
							if (isLogLevelEnabled(LogLevel.DEBUG))
							{
								debug(String.format(
										"Data (%s) from ds (%s) saved to sample"
										, data, dataSource.getName()));
							}
							if (   sampleUpdatePolicy==SampleUpdatePolicy.UPDATE_WHEN_READY
								&& dataSample.isAllValuesSetted())
							{
								saveSampleData();
							}
							break;
					}
                }
            }
            finally
            {
                dbLock.writeLock().unlock();
            }
        } 
        catch (Exception e)
        {
            logger.error(
                    String.format("Error error saving new value to rrd node (%s)", getPath()), e);
        }
	}

	public RRIoQueueNode getIoQueue()
	{
		return ioQueue;
	}

	public void setIoQueue(RRIoQueueNode ioQueue)
	{
		this.ioQueue = ioQueue;
	}

    public String getDatabaseFileName()
    {
        return databaseFileName;
    }

    public void setDatabaseFileName(String databaseFileName)
    {
        this.databaseFileName = databaseFileName;
    }

    public Long getStep()
    {
        return step;
    }

    public void setStep(Long step)
    {
        this.step = step;
//        RrdToolkit.
    }

	public String getStartTime()
	{
		return startTime;
	}

	public void setStartTime(String startTime)
	{
		this.startTime = startTime;
	}

    public Boolean isBackup()
    {
        return backup;
    }

    public void setBackup(Boolean backup)
    {
        this.backup = backup;
    }

	public SampleUpdatePolicy getSampleUpdatePolicy()
	{
		return sampleUpdatePolicy;
	}

	public void setSampleUpdatePolicy(SampleUpdatePolicy sampleUpdatePolicy)
	{
		this.sampleUpdatePolicy = sampleUpdatePolicy;
	}

	@Parameter(readOnly=true)
	public int getValuesInSample()
	{
		return dataSample==null? 0 : dataSample.getValuesCount();
	}

	public long getWritesCount()
	{
		return writesCount;
	}

	@Parameter(readOnly=true)
	public String getLastUpdate() throws Exception
	{
		if (getStatus()==Status.STARTED)
		{
			SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			return fmt.format(new Date(db.getLastUpdateTime()*1000));
		}
		else
			return null;
	}

    private void addArchive(RRArchive archive, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            closeDatabase();
            RrdToolkit.addArchive(databaseFileName, createArcDef(archive), backup);
            openDatabase();
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }

    private void addDataSource(RRDataSource ds, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            closeDatabase();
            RrdToolkit.addDatasource(databaseFileName, createDsDef(ds), backup);
            openDatabase();
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }

    private boolean containsArchive(RRArchive archive) throws IOException
    {
        String conFun = converter.convert(String.class, archive.getConsolidationFunction(), null);
        return db.getArchive(conFun, archive.getSteps())!=null;
    }

    private boolean containsArchiveNode(Archive arc) throws IOException
    {
        if (getChildrens()==null)
            return false;
        
        for (Node node: getChildrens())
            if (   node instanceof RRArchive 
                && arc.getConsolFun().equals(((RRArchive)node).getConsolidationFunction())
                && arc.getSteps()==((RRArchive)node).getSteps() )
            {
                return true;
            }
        
        return false;
    }

    private ArcDef createArcDef(RRArchive archive) throws RrdException
    {
        String conFun = converter.convert(String.class, archive.getConsolidationFunction(), null);
        ArcDef def = new ArcDef(conFun, archive.getXff(), archive.getSteps(), archive.getRows());
        return def;
    }

    private DsDef createDsDef(RRDataSource ds) throws Exception
    {
        if (ds.getHeartbeat()==null)
        {
            NodeAttribute attr = ds.getNodeAttribute("heartbeat");
            attr.setValue(""+(step*2));
            attr.save();
        }
        DsDef def = new DsDef(
                ds.getName(), ds.getDataSourceType().asString(), ds.getHeartbeat()
                , ds.getMinValue(), ds.getMaxValue());
        return def;
    }

    private void createDatabase() throws Exception
    {
        if (getChildrens() == null || getChildrens().size() == 0)
        {
            return;
        }
        
        String rrdDatabasesPath = 
                configurator.getConfig().getStringProperty(Configurator.RRD_DATABASES_PATH, null);

        if (rrdDatabasesPath == null)
        {
            throw new Exception(String.format(
                    "The configuration paramter (%s) must be seted"
                    , Configurator.RRD_DATABASES_PATH));
        }
        File path = new File(rrdDatabasesPath);
        if (!path.exists())
        {
            if (!path.mkdirs())
                throw new Exception(String.format("Error creating directory (%s)", path));
//            FileUtils.forceMkdir(path);
        }
		String databasePath = path.getAbsolutePath() + File.separator + getId() + ".jrb";

        db = createDatabase(databasePath);
		if (db==null)
			return;
		setDatabaseFileName(db.getCanonicalPath());

        databaseInitialized.set(true);
    }

	public RrdDb createDatabase(String databasePath) throws Exception
	{
        RrdDef def = new RrdDef(databasePath, Util.getTimestamp(startTime), step);

        boolean hasDataSources = false;
        boolean hasArchives = false;

        for (Node node : getChildrens())
        {
            if (node instanceof RRDataSource)
            {
                hasDataSources = true;
                RRDataSource ds = (RRDataSource) node;
                def.addDatasource(createDsDef(ds));
            }
            else if (node instanceof RRArchive)
            {
                hasArchives = true;
                RRArchive ar = (RRArchive) node;
                def.addArchive(createArcDef(ar));
            }
        }

        if (!hasDataSources || !hasArchives)
        {
            return null;
        }

        db = new RrdDb(def);

		return db;
	}
    
    private void initDataBase() throws Exception
    {
        dbLock.writeLock().lock();
        try
        {
            databaseInitialized.set(false);
            if (databaseFileName!=null)
            {
                syncDatabase();
            } 
            else
            {
                createDatabase();
            }
//            sample = db.createSample(Util.getTime()+step);
        }finally
        {
            dbLock.writeLock().unlock();
        }
    }

    private void openDatabase() throws IOException, RrdException
    {
        db = new RrdDb(databaseFileName);
        sample = db.createSample(Util.getTime()+step);
    }
    
    private void closeDatabase() throws IOException, RrdException
    {
//        tuneSampleTime();
//        sample.update();
		saveSampleData();
        db.close();
        db = null;
    }
    
	private void saveSampleData() throws IOException, RrdException
	{
		try
		{
			if (dataSample==null || dataSample.getValuesCount()==0)
			{
				if (isLogLevelEnabled(LogLevel.DEBUG))
					debug("Can't save sample data. Nothing to save.");
			}
			else
			{
				if (isLogLevelEnabled(LogLevel.DEBUG))
					debug("Saving sample data to the database. Sample time is "
							+Util.getDate(dataSample.getSampleTime()));
				Sample dbSample = db.createSample(dataSample.getSampleTime());
				for (Map.Entry<RRDataSource, Double> valuesEntry: dataSample.getValues().entrySet())
					dbSample.setValue(valuesEntry.getKey().getName(), valuesEntry.getValue());

				dbSample.update();

				writesCount++;
			}
		}
		finally
		{
			dataSample = null;
		}
	}

    private void tuneSampleTime() throws IOException
    {
        if (sample!=null && db!=null && sample.getTime()<=db.getHeader().getLastUpdateTime())
            sample.setTime(db.getHeader().getLastUpdateTime()+1);
    }

    private void removeArchive(String consolidationFunction, int steps, boolean lock)
			throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            if (getStatus()==Status.STARTED && databaseInitialized.get())
            {
                closeDatabase();
                RrdToolkit.removeArchive(
                        databaseFileName, consolidationFunction, steps, backup);
                openDatabase();
            }
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }
    

    private void removeDataSource(String dsName, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            if (getStatus()==Status.STARTED && databaseInitialized.get())
            {
                closeDatabase();
                RrdToolkit.removeDatasource(databaseFileName, dsName, backup);
                openDatabase();
            }
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }

    private void updateArchive(RRArchive archive, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            String conFun = 
                    converter.convert(String.class, archive.getConsolidationFunction(), null);
            Archive rra = db.getArchive(conFun, archive.getSteps());
            
            double xxf = rra.getXff();
            int rows = rra.getRows();
            
            closeDatabase();
            
            if (xxf!=archive.getXff())
                RrdToolkit.setArcXff(
                        databaseFileName, conFun, archive.getSteps(), archive.getXff());
            if (rows!=archive.getRows())
                RrdToolkit.resizeArchive(
                        databaseFileName, conFun, archive.getSteps(), archive.getRows(), backup);
            
            openDatabase();
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }

    private void updateDataSource(RRDataSource rrds, Datasource ds, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            long heartbeat = ds.getHeartbeat();
            double minValue = ds.getMinValue();
            double maxValue = ds.getMaxValue();
            String dsName = ds.getDsName();
            
            closeDatabase();
            
            if (!dsName.equals(rrds.getName()))
                RrdToolkit.renameDatasource(databaseFileName, dsName, rrds.getName());
            
            if (heartbeat!=rrds.getHeartbeat())
                RrdToolkit.setDsHeartbeat(databaseFileName, rrds.getName(), rrds.getHeartbeat());
            
            if (minValue!=rrds.getMinValue() || maxValue!=rrds.getMaxValue())
                RrdToolkit.setDsMinMaxValue(
                        databaseFileName, rrds.getName(), rrds.getMinValue()
                        , rrds.getMaxValue(), true);
                
            openDatabase();
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }
    
    private void syncDatabase() throws Exception
    {
        openDatabase();

        if (getChildrens()==null)
        {
            db.close();
            new File(databaseFileName).delete();
            databaseInitialized.set(false);
//                createDatabase();
        }
        else
        {
            HashSet<Integer> dsIndexes = new HashSet<Integer>();
            for (Node node: getChildrens())
            {
                if (node instanceof RRDataSource)
                {
                    syncDataSource((RRDataSource) node, false);
                }
                else if (node instanceof RRArchive)
                {
                    RRArchive rra = (RRArchive) node;
                    syncArchive(rra, false);
                }
            }

            //deleting rrd datasources not linked with RRDataSource
            for (int i=0; i<db.getDsCount(); ++i)
            {
                if (getChildren(db.getDatasource(i).getDsName())==null)
                    removeDataSource(db.getDatasource(i).getDsName(), false);
            }

            //deleting rrd archives not linked with RRArchive
            for (int i=0; i<db.getArcCount(); ++i)
            {
                Archive arc = db.getArchive(i);
                if (!containsArchiveNode(arc))
                    removeArchive(arc.getConsolFun(), arc.getSteps(), false);
            }
        }

        databaseInitialized.set(true);
    }
    
    private void addNewDsName(String oldName, String newName)
    {
        newDsNamesLock.lock();
        try
        {
            if (newDsNames==null)
                newDsNames = new HashMap<String, String>();
            
            if (newDsNames.containsKey(oldName))
                oldName = newDsNames.remove(oldName);
            
            newDsNames.put(newName, oldName);
        }
        finally
        {
            newDsNamesLock.unlock();
        }
    }
    
    private String getOldDsName(String newDsName)
    {
        newDsNamesLock.lock();
        try
        {
            String oldDsName = newDsNames==null? null : newDsNames.remove(newDsName);
            return oldDsName==null? newDsName : oldDsName;
        }
        finally
        {
            newDsNamesLock.unlock();
        }
    }
    
    private void syncArchive(RRArchive rra, boolean lock)
    {
        try
        {
            if (lock)
                dbLock.writeLock().lock();
            try 
            {
                if (rra.getStatus()==Status.STARTED && databaseInitialized.get())
                {
                    if (!containsArchive(rra))
                    {
                        addArchive(rra, false);
                    } else
                    {
                        updateArchive(rra, false);
                    }
                }
            }
            finally
            {
                if (lock)
                    dbLock.writeLock().unlock();
            }
        }catch(Exception e)
        {
            throw new NodeError(
                    String.format(
                        "Error synchronzing node (%s) with rrd datasource", rra.getPath())
                    , e);
        }
    }


    private void syncDataSource(RRDataSource rrds, boolean lock) 
    {
        try
        {
            if (lock)
                dbLock.writeLock().lock();
            try 
            {
                if (   rrds.getStatus()==Status.STARTED 
                    && databaseInitialized.compareAndSet(true, false))
                {
                    String dsName = getOldDsName(rrds.getName());
                    Datasource ds = db.getDatasource(dsName);
                    if (ds!=null)
                        updateDataSource(rrds, ds, false);
                    else 
                    {
                        ds = db.getDatasource(rrds.getName());
                        if (ds!=null)
                            updateDataSource(rrds, ds, false);
                        else
                            addDataSource(rrds, false);
                    }
                    databaseInitialized.set(true);
                }
            }
            finally
            {
                if (lock)
                    dbLock.writeLock().unlock();
            }
        }catch(Exception e)
        {
            throw new NodeError(String.format(
                    "Error synchronzing node (%s) with rrd datasource", rrds.getPath())
                    , e);
        }
    }

    @Override
    public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
    {
        super.nodeStatusChanged(node, oldStatus, newStatus);
        
        if (   databaseInitialized.get() 
            && node.getStatus()==Status.STARTED 
            && getStatus()!=Status.REMOVING)
        {
            if (node instanceof RRDataSource)
                syncDataSource((RRDataSource) node, true);
            else if (node instanceof RRArchive)
                syncArchive((RRArchive) node, true);
        }
    }

    @Override
    public void nodeNameChanged(Node node, String oldName, String newName)
    {
        super.nodeNameChanged(node, oldName, newName);
        
        if (node instanceof RRDataSource)
        {
            addNewDsName(oldName, newName);
            if (databaseInitialized.get() && node.getStatus()==Status.CREATED)
                syncDataSource((RRDataSource) node, true);
        }
        else if (node instanceof RRArchive)
        {
            syncArchive((RRArchive) node, true);
        }
    }

    @Override
    public void nodeAttributeValueChanged(
            Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        if (databaseInitialized.get() && node.getStatus()==Status.STARTED)
        {
            if (node instanceof RRDataSource)
                syncDataSource((RRDataSource) node, true);
            else if (node instanceof RRArchive)
                syncArchive((RRArchive) node, true);
        }
    }
}
