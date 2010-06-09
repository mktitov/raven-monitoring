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

package org.raven.ds;

import java.util.Iterator;

/**
 *
 * @author Mikhail Titov
 */
public interface DataStore
{
    /**
     * Opens store for read write operations
     */
    public void open() throws DataStoreException;
    /**
     * Closes store
     */
    public void close() throws DataStoreException;
    /**
     * Releases resources holded by store
     */
    public void release() throws DataStoreException;
    /**
     * Adds data portion to store
     * @param dataPortion the portion of data which must be stored
     */
    public void addDataPortion(Object dataPortion) throws DataStoreException;
    /**
     * Returns the data context of the data added to the store
     */
    public DataContext getDataContext() throws DataStoreException;
    /**
     * Sets the context of data added to the store
     * @param context The data context
     */
    public void setDataContext(DataContext context) throws DataStoreException;
    /**
     * Returns the iterator over data added by method {@link #addDataPortion(java.lang.Object)}
     */
    public Iterator getDataIterator() throws DataStoreException;
}
