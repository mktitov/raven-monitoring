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

package org.raven.ds.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.raven.ds.DataContext;
import org.raven.ds.DataStore;
import org.raven.ds.DataStoreException;

/**
 *
 * @author Mikhail Titov
 */
public class MemoryDataStore implements DataStore
{
    private final List dataList = new ArrayList(128);
    private DataContext dataContext;

    public void open() throws DataStoreException
    {
    }

    public void close() throws DataStoreException
    {
    }

    public void release() throws DataStoreException
    {
        dataList.clear();
    }

    public void addDataPortion(Object dataPortion) throws DataStoreException
    {
        dataList.add(dataPortion);
    }

    public Iterator getDataIterator() throws DataStoreException
    {
        return dataList.iterator();
    }

    public DataContext getDataContext() throws DataStoreException 
    {
        return dataContext;
    }

    public void setDataContext(DataContext context) throws DataStoreException 
    {
        this.dataContext = context;
    }
}
