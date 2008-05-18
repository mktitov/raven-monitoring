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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.FileUtils;
import org.jrobin.core.ArcDef;
import org.jrobin.core.Archive;
import org.jrobin.core.Datasource;
import org.jrobin.core.DsDef;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.RrdToolkit;
import org.jrobin.core.Sample;
import org.jrobin.core.Util;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;

/**
 *
 * @author Mikhail Titov
 */
public class RRDNode extends BaseNode implements DataConsumer, NodeListener
{
    @Parameter 
    @Description("The base interval in seconds with which data will be fed into the RRD")
    private long step = 300;
    
    @Parameter
    @Description("The file name of the rrd database")
    private String databaseFileName;
    
    private AtomicBoolean databaseInitialized = new AtomicBoolean(false);
    private RrdDb db;
    private ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock();
    private Map<String, String> newDsNames;
    private ReentrantLock newDsNamesLock = new ReentrantLock();
    
            
    public RRDNode()
    {
        super(new Class[]{RRDataSource.class, RRArchive.class}, true, false);
        setInitializeAfterChildrens(true);
    }

    @Override
    public synchronized boolean start() throws NodeError
    {
        boolean started =  super.start();
        
        if (started)
        {
            try
            {
                initDataBase();
            } catch (Exception e)
            {
                started = false;
                setStatus(Status.INITIALIZED);
                logger.error(String.format("Error starting node (%s)", getPath()), e);
            }
        }
        
        return started;
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
                    db.close();
                    db = null;
                }
            }
            finally
            {
                dbLock.writeLock().unlock();
            }
        } catch (IOException e)
        {
            logger.error(String.format("Error stoping node (%s)", getPath()), e);
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
            super.removeChildren(node);
            
            if (node instanceof RRDataSource)
            {
                removeDataSource(node.getName(), true);
            } else if (node instanceof RRArchive)
            {
                RRArchive raa = (RRArchive) node;
                removeArchive(raa.getConsolidationFunction(), raa.getSteps(), true);
            }
        } catch (Exception e)
        {
            throw new NodeError(String.format(
                    "Error removing child node (%s) from the (%s) node", node.getPath(), getPath())
                    , e);
        }
    }

    public void setData(DataSource dataSource, Object data)
    {
        try
        {
            long time = Util.getTime();
            dbLock.readLock().lock();
            try
            {
                if (databaseInitialized.get())
                {
                    Sample sample = db.createSample(time);
                    Double value = converter.convert(Double.class, data, null);
                    System.out.println("!!!"+value);
                    sample.setValue(dataSource.getName(), value);
                    sample.update();
                }
            }
            finally
            {
                dbLock.readLock().unlock();
            }
        } 
        catch (Exception e)
        {
            logger.error(
                    String.format("Error error saving new value to rrd node (%s)", getPath()), e);
        }
    }

    public String getDatabaseFileName()
    {
        return databaseFileName;
    }

    public void setDatabaseFileName(String databaseFileName)
    {
        this.databaseFileName = databaseFileName;
    }

    public long getStep()
    {
        return step;
    }

    public void setStep(long step)
    {
        this.step = step;
    }

    private void addArchive(RRArchive archive, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            db.close();
            RrdToolkit.addArchive(databaseFileName, createArcDef(archive), true);
            db = new RrdDb(databaseFileName);
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
            db.close();
            RrdToolkit.addDatasource(databaseFileName, createDsDef(ds), true);
            db = new RrdDb(databaseFileName);
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }

    private boolean containsArchive(RRArchive archive) throws IOException
    {
        return db.getArchive(archive.getConsolidationFunction(), archive.getSteps())!=null;
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
        ArcDef def = new ArcDef(
                archive.getConsolidationFunction(), archive.getXff()
                , archive.getSteps(), archive.getRows());
        return def;
    }

    private DsDef createDsDef(RRDataSource ds) throws Exception
    {
        if (ds.getHeartbeat()==null)
        {
            ds.setHeartbeat(step*2);
            configurator.getTreeStore().saveNodeAttribute(ds.getNodeAttribute("heartbeat"));
        }
        DsDef def = new DsDef(
                ds.getName(), ds.getDataSourceType(), ds.getHeartbeat()
                , ds.getMinValue(), ds.getMaxValue());
        return def;
    }

    private void createDatabase() throws Exception
    {
        if (getChildrens() == null || getChildrens().size() == 0)
        {
            return;
        }
        
        dbLock.writeLock().lock();
        try
        {
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
                FileUtils.forceMkdir(path);
            }
            RrdDef def = new RrdDef(
                    path.getAbsolutePath() + File.separator + getId() + ".jrrd"
                    , Util.getTime(), step);

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
                return;
            }

            db = new RrdDb(def);
            databaseFileName = db.getCanonicalPath();

            configurator.getTreeStore().saveNodeAttribute(getNodeAttribute("databaseFileName"));

            databaseInitialized.set(true);
        }
        finally
        {
            dbLock.writeLock().unlock();
        }
    }
    
    private void initDataBase() throws Exception
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
    }

    private void removeArchive(String consolidationFunction, int steps, boolean lock) throws Exception
    {
        if (lock)
            dbLock.writeLock().lock();
        try
        {
            if (getStatus()==Status.STARTED && databaseInitialized.get())
            {
                db.close();
                RrdToolkit.removeArchive(
                        databaseFileName, consolidationFunction, steps, true);
                db = new RrdDb(databaseFileName);
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
                db.close();
                RrdToolkit.removeDatasource(databaseFileName, dsName, true);
                db = new RrdDb(databaseFileName);
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
            Archive rra = db.getArchive(archive.getConsolidationFunction(), archive.getSteps());
            
            double xxf = rra.getXff();
            int rows = rra.getRows();
            
            db.close();
            
            if (xxf!=archive.getXff())
                RrdToolkit.setArcXff(
                        databaseFileName, archive.getConsolidationFunction()
                        , archive.getSteps(), archive.getXff());
            if (rows!=archive.getRows())
                RrdToolkit.resizeArchive(
                        databaseFileName, archive.getConsolidationFunction(), archive.getSteps()
                        , archive.getRows(), true);
            
            db = new RrdDb(databaseFileName);
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
            
            db.close();
            
            if (!dsName.equals(rrds.getName()))
                RrdToolkit.renameDatasource(databaseFileName, dsName, rrds.getName());
            
            if (heartbeat!=rrds.getHeartbeat())
                RrdToolkit.setDsHeartbeat(databaseFileName, rrds.getName(), rrds.getHeartbeat());
            
            if (minValue!=rrds.getMinValue() || maxValue!=rrds.getMaxValue())
                RrdToolkit.setDsMinMaxValue(
                        databaseFileName, rrds.getName(), rrds.getMinValue()
                        , rrds.getMaxValue(), true);
                
            db = new RrdDb(databaseFileName);
        }
        finally
        {
            if (lock)
                dbLock.writeLock().unlock();
        }
    }
    
    private void syncDatabase() throws Exception
    {
        dbLock.writeLock().lock();
        try
        {
            db = new RrdDb(databaseFileName);

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
//                        if (node.getStatus()==Status.CREATED)
//                        {
////                            Datasource datasource = db.getDatasource(node.getName());
//                            if (!db.containsDs(node.getName()))
//                                addDataSource((RRDataSource)node, false);
//                            else 
//                                updateDataSource((RRDataSource)node, false);
//                        }
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
        finally
        {
            dbLock.writeLock().unlock();
        }
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
            throw new NodeError(String.format(
                    "Error synchronzing node (%s) with rrd datasource", rra.getPath()));
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
                        addDataSource(rrds, false);
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
        
        if (databaseInitialized.get() && node.getStatus()==Status.STARTED)
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
            Node node, NodeAttribute attribute, String oldValue, String newValue)
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
