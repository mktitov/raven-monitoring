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
import java.util.LinkedList;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CollectionComposerNode extends AbstractSafeDataPipe
{
    private ThreadLocal<Collection> collections;

    @Override
    protected void initFields()
    {
        super.initFields();
        collections = new ThreadLocal<Collection>();
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        Collection collection = collections.get();
        if (data==null){
            if (collection!=null){
                sendDataToConsumers(collection, context);
                collections.remove();
            }
        }else{
            if (collection==null){
                collection = new LinkedList();
                collections.set(collection);
            }
            collection.add(data);
        }
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }
}
