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

package org.raven.table;

import java.util.Date;
import java.util.Iterator;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class DataArchiveTable extends TableImpl
{
    @Service
    private static TypeConverter converter;

    public DataArchiveTable()
    {
        super(new String[]{"timestamp", "value"});
    }

    /**
     * Add data with corresponding timestamp to the table.
     * @param timestamp the value timestamp
     * @param value the data
     */
    public void addData(Date timestamp, Object value)
    {
        addRow(new Object[]{timestamp, value});
    }

    public Object sum() throws ConsolidationFunctionException
    {
        SumFunction sum = new SumFunction();
        sum.startCalculation(Double.class);
        for (Iterator<Object[]> row=getRowIterator(); row.hasNext();)
        {
            Object value = row.next()[1];
            sum.nextCalculation(converter.convert(Double.class, value, null));
        }
        sum.finishCalculation();
        return sum.getResultValue();
    }
}
