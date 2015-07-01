/*
 *  Copyright 2010 Mikhail Titov.
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

import java.util.Collection;
import java.util.Iterator;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.table.Table;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CollectionDecomposerNode extends AbstractSafeDataPipe
{
    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (data instanceof Collection)
            processIterator(((Collection)data).iterator(), context);
        else if (data instanceof Iterable)
            processIterable((Iterable)data, context);
        else if (data instanceof Table)
            processIterator(((Table)data).getRowIterator(), context);
        else if (data instanceof Iterator)
            processIterator((Iterator) data,context);
        else 
            sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    private void processIterator(Iterator it, DataContext context)
    {
        while (it.hasNext())
            sendDataToConsumers(it.next(), context);
    }

    private void processIterable(Iterable iterable, DataContext context)
    {
        for (Object o: iterable)
            sendDataToConsumers(o, context);
    }
}
