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

package org.raven.tree.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import org.raven.conf.Configurator;
import org.raven.tree.AttributesGenerator;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class DataFileValueHandler extends AbstractAttributeValueHandler
        implements DataFile, AttributesGenerator
{
    public static final String FILENAME_SUFFIX = "filename";
    public static final String MIMETYPE_SUFFIX = "mimeType";

    private boolean firstHandleData = true;

    @Service
    private static Configurator configurator;

    public DataFileValueHandler(NodeAttribute attribute) throws DataFileValueHandlerException
    {
        super(attribute);

        if (!DataFile.class.isAssignableFrom(attribute.getType()))
            throw new DataFileValueHandlerException(String.format(
                    "Invalid attribute (%s) type (%s). Type must be assignable from (%s)"
                    , attribute.getPath()
                    , attribute.getType().getName(), DataFile.class.getName()));
    }

    public void setData(String value) throws Exception
    {
    }

    public String getData()
    {
        return null;
    }

    @SuppressWarnings("empty-statement")
    public Object handleData()
    {
        if (firstHandleData)
        {
            firstHandleData = false;
//            fireValueChangedEvent(null, this);
        }
        return this;
    }

    public void close()
    {
    }

    public boolean isReferenceValuesSupported()
    {
        return false;
    }

    public boolean isExpressionSupported()
    {
        return true;
    }

    public boolean isExpressionValid()
    {
        return true;
    }

    public void validateExpression() throws Exception
    {
    }

    public String getFilename() throws DataFileException
    {
        return getFileAttribute(FILENAME_SUFFIX).getValue();
    }

    public void setFilename(String filename) throws DataFileException
    {
        NodeAttribute filenameAttr = getFileAttribute(FILENAME_SUFFIX);
        try
        {
            filenameAttr.setValue(filename);
            filenameAttr.save();
        }
        catch (Exception ex)
        {
            throw new DataFileException(String.format(
                    "Error seting value for filename attribute (%s)", filenameAttr.getPath()));
        }
    }

    public String getMimeType() throws DataFileException
    {
        return getFileAttribute(MIMETYPE_SUFFIX).getValue();
    }

    public void setMimeType(String mimeType) throws DataFileException
    {
        NodeAttribute mimeTypeAttr = getFileAttribute(MIMETYPE_SUFFIX);
        try
        {
            mimeTypeAttr.setValue(mimeType);
            mimeTypeAttr.save();
        }
        catch (Exception ex)
        {
            throw new DataFileException(String.format(
                    "Error seting value for mime type attribute (%s)", mimeTypeAttr.getPath()));
        }
    }

    public InputStream getDataStream()
    {
        return configurator.getTreeStore().getNodeAttributeBinaryData(attribute);
    }

    public void setDataStream(InputStream data)
    {
        configurator.getTreeStore().saveNodeAttributeBinaryData(attribute, data);
        fireValueChangedEvent(null, null);
    }

    private NodeAttribute getFileAttribute(String suffix) throws DataFileException
    {
        Node owner = attribute.getOwner();
        String attrName = attribute.getName()+"."+suffix;
        NodeAttribute fileAttr = owner.getNodeAttribute(attrName);
        if (fileAttr==null)
            throw new DataFileException(String.format(
                    "Attribute (%s) not found in the node (%s)"
                    , attrName, owner.getPath()));
        return fileAttr;
    }

    public Collection<NodeAttribute> generateAttributes()
    {
        NodeAttribute filenameAttr = new NodeAttributeImpl(
                attribute.getName()+"."+FILENAME_SUFFIX, String.class, null, null);

        NodeAttribute mimeTypeAttr = new NodeAttributeImpl(
                attribute.getName()+"."+MIMETYPE_SUFFIX, String.class, null, null);

        return Arrays.asList(filenameAttr, mimeTypeAttr);
    }

    private boolean hasFileAttributes()
    {
        Node owner = attribute.getOwner();
        NodeAttribute attr = owner.getNodeAttribute(attribute.getName()+"."+FILENAME_SUFFIX);
        if (attr==null)
            return false;
        attr = owner.getNodeAttribute(attribute.getName()+"."+MIMETYPE_SUFFIX);
        return attr!=null;
    }

    @Override
    public String toString()
    {
        if (!configurator.getTreeStore().hasNodeAttributeBinaryData(attribute))
            return null;
        else
        {
            String filename = null;
            String mimetype = null;
            try {filename = getFileAttribute(FILENAME_SUFFIX).getValue();} catch (Exception e){}
            try {mimetype = getFileAttribute(MIMETYPE_SUFFIX).getValue();} catch (Exception e){}

            StringBuilder str = new StringBuilder();
            if (filename!=null)
                str.append("filename: "+filename);
            if (mimetype!=null)
            {
                if (filename!=null)
                    str.append(", ");
                str.append("mime-type: "+mimetype);
            }
            return str.toString();
        }
    }
}
