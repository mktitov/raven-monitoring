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

package org.raven.ds;

import org.raven.table.DataArchiveTable;

/**
 *
 * @author Mikhail Titov
 */
public interface DataArchive
{
    /**
     * Returns the list of the data for the period [fromDate, toDate]
     * @param fromDate the starting date in the period
     * @param toDate the end of the period
     */
    public DataArchiveTable getArchivedData(String fromDate, String toDate) throws ArchiveException;
}
