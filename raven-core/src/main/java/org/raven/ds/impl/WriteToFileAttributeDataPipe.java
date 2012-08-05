/*
 * Copyright 2012 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.ds.impl;

import java.io.InputStream;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.expr.BindingSupport;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class WriteToFileAttributeDataPipe extends AbstractSafeDataPipe {
    
    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private Node node;
    @NotNull @Parameter
    private String fileAttribute;
    @NotNull @Parameter
    private String fileName;
    @NotNull @Parameter
    private String mimeType;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public String getFileAttribute() {
        return fileAttribute;
    }

    public void setFileAttribute(String fileAttribute) {
        this.fileAttribute = fileAttribute;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception {
        if (data!=null) {
            InputStream stream = converter.convert(InputStream.class, data, null);
            try {
                bindingSupport.put(DATA_BINDING, data);
                bindingSupport.put(DATASOURCE_BINDING, dataSource);
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                Node sourceNode = node;
                if (sourceNode==null) 
                    throw new Exception("Node attribute value can not be null");
                String attrName = fileAttribute;
                if (attrName==null)
                    throw new Exception("fileAttribute attribute value can not be null");
                String _fileName = fileName;
                if (_fileName==null)
                    throw new Exception("fileName attribute value can not be null");
                String _mimeType = mimeType;
                if (_mimeType==null)
                    throw new Exception("mimeType attribute value can not be null");
                NodeAttribute attr = sourceNode.getNodeAttribute(attrName);
                if (!DataFile.class.isAssignableFrom(attr.getType()))
                    throw new Exception(String.format("Invalid type if the attribute (%s). "
                            + "The attribute must be of type DataFile, but the type is (%s) "
                            , attrName, attr.getType().getName()));
                DataFile dataFile  = attr.getRealValue();
                dataFile.setDataStream(stream);
                dataFile.setFilename(_fileName);
                dataFile.setMimeType(_mimeType);
            } finally {
                bindingSupport.reset();
            }
        }
        sendDataToConsumers(data, context);
    }

    @Override
    protected void doAddBindingsForExpression(DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport) {
    }
}
