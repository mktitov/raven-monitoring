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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.raven.conf.Configurator;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class DataFileValueHandler extends AbstractAttributeValueHandler implements DataFile
{
    public static final String FILENAME_SUFFIX = "filename";
    public static final String MIMETYPE_SUFFIX = "mimeType";
    public static final String SIZE_SUFFIX = "filesize";
    public static final String CHECKSUM_SUFFIX = "checksum";
    public static final String ENCODING_SUFFIX = "encoding";

    @Service
    private static MessagesRegistry messagesRegistry;

    @Service
    private static Configurator configurator;

    private boolean firstHandleData = true;

    public DataFileValueHandler(NodeAttribute attribute) throws DataFileValueHandlerException {
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
            firstHandleData = false;
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
        return getOrCreateAttribute(FILENAME_SUFFIX, String.class).getValue();
    }

    public void setFilename(String filename) throws DataFileException
    {
//        NodeAttribute filenameAttr = getFileAttribute(FILENAME_SUFFIX);
        NodeAttribute filenameAttr = getOrCreateAttribute(FILENAME_SUFFIX, String.class);
        try
        {
            filenameAttr.setValue(filename);
            filenameAttr.save();
        }
        catch (Exception ex)
        {
            throw new DataFileException(
                    String.format(
                        "Error seting value for filename attribute (%s)", filenameAttr.getPath())
                    , ex);
        }
    }

    public String getMimeType() throws DataFileException
    {
        return getOrCreateAttribute(MIMETYPE_SUFFIX, String.class).getValue();
    }

    public void setMimeType(String mimeType) throws DataFileException
    {
//        NodeAttribute mimeTypeAttr = getFileAttribute(MIMETYPE_SUFFIX);
        NodeAttribute mimeTypeAttr = getOrCreateAttribute(MIMETYPE_SUFFIX, String.class);
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

    public Charset getEncoding() throws DataFileException {
        return getOrCreateAttribute(ENCODING_SUFFIX, Charset.class).getRealValue();
    }

    public void setEncoding(Charset encoding) throws DataFileException {
        NodeAttribute attr = getOrCreateAttribute(ENCODING_SUFFIX, Charset.class);
        try {
            attr.setValue(encoding!=null?encoding.name():null);
        } catch (Exception e) {
            throw new DataFileException(String.format(
                    "Error stting value for encoding attribute (%s)", attr.getPath()));
        }
    }
    
    public Long getFileSize() throws DataFileException
    {
        return getOrCreateAttribute(SIZE_SUFFIX, Long.class).getRealValue();
    }

    public Long getChecksum() throws DataFileException {
        NodeAttribute attr = getOrCreateAttribute(CHECKSUM_SUFFIX, Long.class);
        Long val = attr.getRealValue();
        if (val==null || val==0){
            InputStream is = getDataStream();
            if (is!=null){
                byte[] buf = new byte[1024];
                CheckedInputStream checksumStream = new CheckedInputStream(is, new Adler32());
                try {
                    try {
                        while (checksumStream.read(buf) != -1) ;
                        attr.setValue(""+checksumStream.getChecksum().getValue());
                        attr.save();
                        val = checksumStream.getChecksum().getValue();
                    } finally {
                        is.close();
                    }
                } catch (Exception ex) {
                    throw new DataFileException("Error calculating checksum", ex);
                }
            }
        }
        return val;
    }

    public InputStream getDataStream() {
        return configurator.getTreeStore().getNodeAttributeBinaryData(attribute);
    }
    
    public Reader getDataReader() throws DataFileException {
        Charset encoding = getEncoding();
        if (encoding==null)
            encoding = Charset.defaultCharset();
        InputStream dataStream = getDataStream();
        return dataStream==null? null : new InputStreamReader(dataStream, encoding);
    }

    public void setDataStream(InputStream data) throws DataFileException
    {
        CountingInputStream countingStream = null;
        CheckedInputStream checksumStream = null;
        if (data!=null){
            countingStream = new CountingInputStream(data);
            checksumStream = new CheckedInputStream(countingStream, new Adler32());
        }
        try
        {
            configurator.getTreeStore().saveNodeAttributeBinaryData(attribute, checksumStream);
            if (data!=null)
            {
                NodeAttribute attr = getOrCreateAttribute(SIZE_SUFFIX, Long.class);
                attr.setValue("" + countingStream.getByteCount());
                attr.save();
                attr = getOrCreateAttribute(CHECKSUM_SUFFIX, Long.class);
                attr.setValue("" + checksumStream.getChecksum().getValue());
                attr.save();
            }
            fireValueChangedEvent(null, null);
        }
        catch (Exception ex)
        {
            throw new DataFileException(
                    String.format(
                    "Error saving binary data for attribute (%s)", attribute.getName())
                    , ex);
        }
    }

    public void setDataString(String data) throws DataFileException {
        Charset encoding = getEncoding();
        if (encoding==null)
            encoding = Charset.defaultCharset();
        InputStream stream = data==null? null : new ByteArrayInputStream(data.getBytes(encoding));
        setDataStream(stream);
    }
    
    private NodeAttribute getFileAttribute(String suffix) throws DataFileException
    {
        Node owner = attribute.getOwner();
        String attrName = attribute.getName()+"."+suffix;
        NodeAttribute fileAttr = owner.getAttr(attrName);
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

        NodeAttribute sizeAttr = new NodeAttributeImpl(
                attribute.getName()+"."+SIZE_SUFFIX, Long.class, null, null);

        NodeAttribute checksumAttr = new NodeAttributeImpl(
                attribute.getName()+"."+CHECKSUM_SUFFIX, Long.class, null, null);
        
        NodeAttribute encodingAttr = new NodeAttributeImpl(
                attribute.getName()+"."+ENCODING_SUFFIX, Charset.class, null, null);

        return Arrays.asList(filenameAttr, mimeTypeAttr, sizeAttr, checksumAttr, encodingAttr);
    }

    private NodeAttribute getOrCreateAttribute(String suffix, Class type) throws DataFileException
    {
        String name = attribute.getName()+"."+suffix;
        Node owner = attribute.getOwner();
        NodeAttribute attr = owner.getAttr(name);
        if (attr==null)
        {
            try
            {
                attr = new NodeAttributeImpl(name, type, null, null);
                attr.setParentAttribute(attribute.getName());
                attr.setOwner(owner);
                attr.init();
                owner.addNodeAttribute(attr);
                String descKey = messagesRegistry.createMessageKeyForStringValue(
                        this.getClass().getName(), suffix);
                attr.setDescriptionContainer(new MessageComposer(messagesRegistry).append(descKey));
                attr.save();
            } catch (Exception ex)
            {
                throw new DataFileException(
                        String.format("Error creating attribute (%s)", name)
                        , ex);
            }
        }
        return attr;
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
                str.append("filename: ").append(filename);
            if (mimetype!=null)
            {
                if (filename!=null)
                    str.append(", ");
                str.append("mime-type: ").append(mimetype);
            }
            return str.toString();
        }
    }
}
