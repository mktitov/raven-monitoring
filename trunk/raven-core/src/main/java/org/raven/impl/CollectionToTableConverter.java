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

package org.raven.impl;

import java.util.Collection;
import org.raven.table.Table;
import org.raven.table.TableImpl;
import org.weda.converter.impl.AbstractConverter;

/**
 *
 * @author Mikhail Titov
 */
public class CollectionToTableConverter extends AbstractConverter<Collection, Table>
{
    public Table convert(Collection rows, Class realTargetType, String format)
    {
        TableImpl table = null;
        for (Object row: rows)
        {
            Object[] vals = null;
            if (row instanceof Object[])
                vals = (Object[]) row;
            else if (row instanceof Collection)
                vals = ((Collection)row).toArray();
            else
                vals = new Object[]{row};

            if (table==null)
            {
                String[] colnames = new String[vals.length];
                for (int i=0; i<colnames.length; ++i)
                    colnames[i] = ""+vals[i];
                table = new TableImpl(colnames);
            }
            else
                table.addRow(vals);
        }

        return table;
    }

    public Class getSourceType()
    {
        return Collection.class;
    }

    public Class getTargetType()
    {
        return Table.class;
    }
}
