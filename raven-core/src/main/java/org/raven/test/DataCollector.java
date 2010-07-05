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
    private List<Object> dataList = new ArrayList<Object>();
    private long pauseBeforeRecieve = 0;
    private DataHandler dataHandler;

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

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        try {
            if (pauseBeforeRecieve > 0) {
                Thread.sleep(pauseBeforeRecieve);
            }
            if (dataHandler!=null)
                dataHandler.handleData(data, context);
            else
                dataList.add(data);
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
