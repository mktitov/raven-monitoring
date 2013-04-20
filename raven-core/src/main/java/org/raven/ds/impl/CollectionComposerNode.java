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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class CollectionComposerNode extends AbstractSafeDataPipe
{
    public final static String COLLECTION_PARAM = "collection";
    
    @NotNull @Parameter(defaultValue="true")
    private Boolean collectDataInContext;
    
    private List dataList;

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
            throws Exception
    {
        Collection dataToSend = null;
        synchronized(this) {
            if (data==null) dataToSend = removeCollection(context);
            else getOrCreateCollection(context).add(data);
        }
        if (data==null) {
            if (dataToSend!=null)
                sendDataToConsumers(dataToSend, context);
            sendDataToConsumers(null, context);
        }
    }
    
    private Collection removeCollection(DataContext context) {
        if (collectDataInContext) 
            return (Collection)context.removeNodeParameter(this, COLLECTION_PARAM);
        else {
            Collection collection = null;
            if (dataList!=null) {
                collection = new ArrayList(dataList);
                dataList = null;
            }
            return collection;
        }
    }
    
    private Collection getOrCreateCollection(DataContext context) {
        Collection collection = null;
        if (collectDataInContext) {
            collection = (Collection) context.getNodeParameter(this, COLLECTION_PARAM);
            if (collection==null) {
                collection = new LinkedList();
                context.putNodeParameter(this, COLLECTION_PARAM, collection);
            }
        } else {
            if (dataList==null) dataList = new LinkedList();
            collection = dataList;
        }
        return collection;
    }
    
    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    public Boolean getCollectDataInContext() {
        return collectDataInContext;
    }

    public void setCollectDataInContext(Boolean collectDataInContext) {
        this.collectDataInContext = collectDataInContext;
    }
}
