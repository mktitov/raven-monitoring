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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.tree.DataFile;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataFileValueHandlerFactory;
import org.raven.tree.impl.DataFileViewableObject;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class XslTransformerNode extends AbstractSafeDataPipe implements Viewable
{
    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile stylesheet;

    public DataFile getStylesheet()
    {
        return stylesheet;
    }

    public void setStylesheet(DataFile stylesheet)
    {
        this.stylesheet = stylesheet;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        if (data==null)
            throw new Exception("Can not transform NULL data");
        InputStream in = converter.convert(InputStream.class, data, null);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(
                new StreamSource(stylesheet.getDataStream()));
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        transformer.transform(new StreamSource(in), new StreamResult(out));
        sendDataToConsumers(out.toByteArray(), context);
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception 
    {
        return null;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (!Status.STARTED.equals(getStatus()))
            return null;

        return Arrays.asList((ViewableObject)new DataFileViewableObject(stylesheet, this));
    }

    public Boolean getAutoRefresh() {
        return true;
    }
}
