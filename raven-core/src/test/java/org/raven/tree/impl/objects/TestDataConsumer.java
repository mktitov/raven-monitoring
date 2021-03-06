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

package org.raven.tree.impl.objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.raven.RavenRuntimeException;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractDataConsumer;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataConsumer extends AbstractDataConsumer
{
    private DataSource ds;
    private List dataList = new ArrayList();

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        ds = dataSource;
        if (data==null)
            dataList.add(null);
        else
            try
        {
            dataList.add(IOUtils.toString((InputStream) data));
        } catch (IOException ex)
        {
            throw new RavenRuntimeException("error", ex);
        }
    }

    public List getDataList() {
        return dataList;
    }

    public DataSource getDs() {
        return ds;
    }

    public void reset()
    {
        ds = null;
        data = null;
    }
}
