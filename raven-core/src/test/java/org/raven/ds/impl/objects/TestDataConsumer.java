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

package org.raven.ds.impl.objects;

import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.tree.NodeError;
import org.raven.tree.impl.LeafNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataConsumer extends LeafNode implements DataConsumer
{
    @Parameter @NotNull
    private DataSource ds;

    @Override
    public void start() throws NodeError
    {
        super.start();
        ds.addDataConsumer(this);
    }
    
    public void setData(Object data)
    {
    }

    public DataSource getDs()
    {
        return ds;
    }

    public void setDs(DataSource ds)
    {
        this.ds = ds;
    }
}
