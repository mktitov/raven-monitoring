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
import org.apache.commons.io.FileUtils;
import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDef;
import org.jrobin.core.Sample;
import org.raven.annotations.Parameter;
import org.raven.conf.Configurator;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeError;
import org.raven.tree.NodeListener;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.Description;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class RRDNode extends BaseNode implements DataConsumer, NodeListener
{
    @Service
    private Configurator configurator;
    @Service 
    private TypeConverter converter;
    
    @Parameter 
    @Description("The base interval in seconds with which data will be fed into the RRD")
    private long step = 300;
    
    @Parameter
    @Description("The file name of the rrd database")
    private String databaseFileName;
    private boolean databaseInitialized;
    private RrdDb db;
    
    public RRDNode()
    {
        super(new Class[]{RRDataSource.class, RRArchive.class}, true, false);
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
            if (databaseInitialized)
            {
                databaseInitialized = false;
                db.close();
                db = null;
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

    public void setData(DataSource dataSource, Object data)
    {
        try
        {
            if (databaseInitialized)
            {
                Sample sample = db.createSample();
                Double value = converter.convert(Double.class, data, null);
                sample.setValue(dataSource.getName(), value);
                sample.update();
        
            }
        } catch (Exception e)
        {
        }
    }

    public synchronized void statusChanged(Node node, Status oldStatus, Status newStatus)
    {
//        throw new UnsupportedOperationException("Not supported yet.");
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

    private void initDataBase() throws Exception
    {
        databaseInitialized = false;
        if (databaseFileName!=null)
        {
            
            databaseInitialized = true;
        } 
        else
        {
            if (getChildrens()==null || getChildrens().size()==0)
                return;
            
            String rrdDatabasesPath = configurator.getConfig().getStringProperty(
                    Configurator.RRD_DATABASES_PATH, null);
            
            if (rrdDatabasesPath==null)
                throw new Exception(String.format(
                        "The configuration paramter (%s) must be seted"
                        , Configurator.RRD_DATABASES_PATH));
            
            File path = new File(rrdDatabasesPath);
            if (!path.exists())
                FileUtils.forceMkdir(path);
            
            RrdDef def = new RrdDef(path.getAbsolutePath()+File.separator+getId()+".jrrd", step);
            
            boolean hasDataSources = false;
            boolean hasArchives = false;
            
            for (Node node: getChildrens())
            {
                if (node instanceof RRDataSource)
                {
                    hasDataSources = true;
                    RRDataSource ds = (RRDataSource) node;
                    long heartbeat = ds.getHeartbeat()==null? step*2 : ds.getHeartbeat();
                    def.addDatasource(
                            ds.getName(), ds.getDataSourceType(), heartbeat
                            , ds.getMinValue(), ds.getMaxValue());
                }
                else if (node instanceof RRArchive)
                {
                    hasArchives = true;
                    RRArchive ar = (RRArchive) node;
                    def.addArchive(
                            ar.getConsolidationFunction(), ar.getXff()
                            , ar.getSteps(), ar.getRows());
                }
            }
            
            if (!hasDataSources || !hasArchives)
                return;
            
            db = new RrdDb(def);
            databaseFileName = db.getCanonicalPath();
            
            configurator.getTreeStore().saveNodeAttribute(getNodeAttribute("databaseFileName"));
            
            databaseInitialized = true;
        }
    }
}
