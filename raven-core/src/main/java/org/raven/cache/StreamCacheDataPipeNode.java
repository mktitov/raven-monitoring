/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.cache;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.UserContext;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.BindingSupport;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class StreamCacheDataPipeNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter(defaultValue="SEQUNCE")
    private CacheKeyGenerationPolicy cacheKeyGenerationPolicy;

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private TemporaryFileManager temporaryFileManager;

    private AtomicLong sequence;

    public CacheKeyGenerationPolicy getCacheKeyGenerationPolicy() {
        return cacheKeyGenerationPolicy;
    }

    public void setCacheKeyGenerationPolicy(CacheKeyGenerationPolicy cacheKeyGenerationPolicy) {
        this.cacheKeyGenerationPolicy = cacheKeyGenerationPolicy;
    }

    public TemporaryFileManager getTemporaryFileManager() {
        return temporaryFileManager;
    }

    public void setTemporaryFileManager(TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        sequence = new AtomicLong(1);
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
            throws Exception
    {
        InputStream in = converter.convert(InputStream.class, data, null);
        if (in!=null) {
            StringBuilder key = new StringBuilder().append(getId()).append("_");
            switch (cacheKeyGenerationPolicy) {
                case DATASOURCE_NAME: key.append(dataSource.getPath()); break;
                case DATASOURCE_NAME_AND_USER_NAME:
                    key.append(dataSource.getPath());
                    UserContext userContext = context.getUserContext();
                    if (userContext!=null)
                        key.append("_").append(userContext.getUsername());
                    break;
                default:
                    sequence.compareAndSet(Long.MAX_VALUE, 1);
                    key.append(sequence.getAndIncrement());
            }
            data = temporaryFileManager.saveFile(this, key.toString(), in, null, true);
        }
        sendDataToConsumers(data, context);
        return;
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }
}
