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

package org.raven.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;

/**
 *
 * @author Mikhail Titov
 */
public class DataCollector extends AbstractDataConsumer
{
    private final List<Object> dataList = Collections.synchronizedList(new ArrayList<Object>());
    private long pauseBeforeRecieve = 0;
    private DataHandler dataHandler;
    private DataContext lastDataContext;

    public DataContext getLastDataContext() {
        return lastDataContext;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    public long getPauseBeforeRecieve()
    {
        return pauseBeforeRecieve;
    }

    public void setPauseBeforeRecieve(long pauseBeforeRecieve)
    {
        this.pauseBeforeRecieve = pauseBeforeRecieve;
    }
    
    public boolean waitForData(long timeout) throws InterruptedException {
        return waitForData(timeout, 1);
    }

    public boolean waitForData(long timeout, int dataCount) throws InterruptedException {
        if (dataList.size()>=dataCount) return true;
        else {
            synchronized(this){
                if (dataList.size()>=dataCount) return true;
                else {
                    long waitFor = System.currentTimeMillis()+timeout;
                    do {
                        wait(timeout);
                        if (dataList.size()>=dataCount) return true;
                    } while (waitFor>System.currentTimeMillis());
                    return false;
                }
            }
        }
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)            
    {
        getLogger().debug(String.format("Received data (%s) from (%s)", data, dataSource));
        lastDataContext = context;
        try {
            if (pauseBeforeRecieve > 0) {
                Thread.sleep(pauseBeforeRecieve);
            }
            if (dataHandler!=null)
                dataHandler.handleData(data, context);
            else
                dataList.add(data);
            synchronized(this) {
                notify();
            }
        }
        catch (InterruptedException ex)
        {
        }
    }

    public  List<Object> getDataList()
    {
        return dataList;
    }

    public int getDataListSize()
    {
        return dataList.size();
    }
}
