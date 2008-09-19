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

package org.raven.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.raven.table.Table;

/**
 *
 * @author Mikhail Titov
 */
public class RavenUtils
{
    private RavenUtils(){ }

    public static List<Object[]> tableAsList(Table table)
    {
        if (table==null)
            return null;
        else
        {
            List<Object[]> result = new ArrayList<Object[]>();
            for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
                result.add(it.next());

            return result;
        }
    }

}
