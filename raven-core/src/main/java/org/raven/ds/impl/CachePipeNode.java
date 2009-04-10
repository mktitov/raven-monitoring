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

import java.util.Collection;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;
import org.weda.internal.CacheScope;
import org.weda.internal.annotations.Message;
import org.weda.internal.annotations.Service;
import org.weda.internal.services.CacheManager;

/**
 *
 * @author Mikhail Titov
 */
public class CachePipeNode extends AbstractDataPipe
{
    public static final String CONSUMER_TYPE_ATTR = "consumerType";
    
    @Service
    protected static CacheManager cacheManager;

    @Parameter(defaultValue="SESSION")
    @NotNull
    private CacheScope cacheScope;

    @Message
    private String consumerTypeAttrDescription;

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttribute attr = new NodeAttributeImpl(
                CONSUMER_TYPE_ATTR, Boolean.class, false, consumerTypeAttrDescription);
        consumerAttributes.add(attr);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getDataImmediate(
            DataConsumer dataConsumer, Collection<NodeAttribute> sessionAttributes)
    {
        return super.getDataImmediate(dataConsumer, sessionAttributes);
    }



}
