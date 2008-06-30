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

package org.raven.ds.impl;

import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.impl.ContainerNode;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 * Data consumer that collects data from one data source.
 * 
 * @author Mikhail Titov
 */
public abstract class AbstractDataConsumer extends ContainerNode implements DataConsumer
{
    public final static String DATASOURCE_ATTRIBUTE = "dataSource";
    
    @Parameter @NotNull @Description("The data source")
    private DataSource dataSource;

    public DataSource getDataSource()
    {
        return dataSource;
    }
}
