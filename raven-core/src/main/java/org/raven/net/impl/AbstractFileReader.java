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

package org.raven.net.impl;

import javax.script.Bindings;
import org.raven.net.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.expr.impl.BindingSupportImpl;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractFileReader extends AbstractDataSource
{
    public static final String FILENAME_BINDING = "fileName";
    public static final String URL_ATTRIBUTE = "url";
    public static final String REGEXP_FILEMASK_ATTRIBUTE = "regexpFileMask";
    public static final String REMOVEFILEAFTERPROCESSING_ATTRIBUTE = "removeFileAfterProcessing";
    public static final String ADDFILENAMETOSTREAM_ATTRIBUTE = "addFilenameToStream";
    public static final String FILENAMEENCODING_ATTRIBUTE = "filenameEncoding";

    @Service
    protected static MessagesRegistry messages;

    private BindingSupportImpl bindingSupport;

    @Override
    protected void initFields()
    {
        super.initFields();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        String url = attributes.get(URL_ATTRIBUTE).getRealValue();
        String fileMask = attributes.get(REGEXP_FILEMASK_ATTRIBUTE).getRealValue();
        Boolean removeAfterProcessing =
                attributes.get(REMOVEFILEAFTERPROCESSING_ATTRIBUTE).getRealValue();
        Boolean addFilenameToStream = attributes.get(ADDFILENAMETOSTREAM_ATTRIBUTE).getRealValue();
        NodeAttribute fileEncodingAttr = attributes.get(FILENAMEENCODING_ATTRIBUTE);
        Charset fileEncoding = Charset.defaultCharset();
        if (fileEncoding!=null)
            fileEncoding = fileEncodingAttr.getRealValue();

		if (isLogLevelEnabled(LogLevel.DEBUG))
			debug(String.format(
					"Reading file(s) for data consumer (%s) using url (%s)"
					, dataConsumer.getPath(), url));

        FileWrapper file = resolveFile(url, attributes);

        FileWrapper[] files = null;
        if (file.getType()==FileWrapper.FileType.FILE)
            files = new FileWrapper[]{file};
        else
        {
            files = getChildrens(file, attributes);
            if (fileMask!=null && files!=null)
            {
                FilenameFilter filter = null;
                filter = new RegexpFilenameFilter(fileMask, getLogger());
                List<FileWrapper> filesList = new ArrayList<FileWrapper>(files.length);
                for (FileWrapper fileWrapper: files)
                    if (filter.filter(fileWrapper.getName()))
                        filesList.add(fileWrapper);
                if (filesList.size()>0)
                {
                    files = new FileWrapper[filesList.size()];
                    filesList.toArray(files);
                }
            }
        }

        if (files!=null && files.length>0)
        {
            try
            {
                for (FileWrapper fileWrapper: files)
                    if (fileWrapper.getType()==FileWrapper.FileType.FILE)
                        processFile(
                                dataConsumer, fileWrapper, addFilenameToStream, fileEncoding
                                , removeAfterProcessing);
            }
            catch (Throwable e)
            {
                logger.error(String.format(
                        "Error processing file(s) for data consumer (%s) using url (%s). %s"
                        , dataConsumer.getPath(), file.getName(), e.getMessage()), e);
                
            }
        }
        else
        {
            if (isLogLevelEnabled(LogLevel.DEBUG))
                logger.debug(String.format(
                        "No files found for data consumer (%s) for url (%s) using regexp " +
						"file name filter (%s)"
                        , dataConsumer.getPath(), url, fileMask));
        }
        
        return true;
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttribute attr = new NodeAttributeImpl(
                URL_ATTRIBUTE, String.class, null, getUrlDescription());
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(REGEXP_FILEMASK_ATTRIBUTE, String.class, null, null);
        attr.setDescriptionContainer(createDesc(REGEXP_FILEMASK_ATTRIBUTE));
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                REMOVEFILEAFTERPROCESSING_ATTRIBUTE, Boolean.class, false, null);
        attr.setRequired(true);
        attr.setDescriptionContainer(createDesc(REMOVEFILEAFTERPROCESSING_ATTRIBUTE));
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                ADDFILENAMETOSTREAM_ATTRIBUTE, Boolean.class, false, null);
        attr.setDescriptionContainer(createDesc(ADDFILENAMETOSTREAM_ATTRIBUTE));
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                FILENAMEENCODING_ATTRIBUTE, Charset.class, Charset.defaultCharset(), null);
        attr.setDescriptionContainer(createDesc(FILENAMEENCODING_ATTRIBUTE));
        consumerAttributes.add(attr);
    }

    private MessageComposer createDesc(String attrName)
    {
        return new MessageComposer(messages).append(
                messages.createMessageKeyForStringValue(
                    AbstractFileReader.class.getName(), attrName));
    }

    protected abstract String getUrlDescription();

    protected abstract FileWrapper resolveFile(String url, Map<String, NodeAttribute> attrs)
        throws FileReaderException;

    protected abstract FileWrapper[] getChildrens(
            FileWrapper file, Map<String, NodeAttribute> attributes)
        throws FileReaderException;

    private void processFile(
            DataConsumer dataConsumer, FileWrapper file,
            Boolean addFilenameToStream, Charset fileEncoding, Boolean removeAfterProcessing)
        throws Exception
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            logger.debug(String.format("Proccessing file (%s)", file.getName()));
        InputStream is = file.getInputStream();
        if (addFilenameToStream)
        {
            byte[] filename = new String(file.getName()+"\n").getBytes(fileEncoding);
            ByteArrayInputStream filenameIs = new ByteArrayInputStream(filename);
            is = new SequenceInputStream(filenameIs, is);
        }
        bindingSupport.put(FILENAME_BINDING, file.getName());
        tree.addGlobalBindings(generateBindingSupportId(), bindingSupport);
        try
        {
            dataConsumer.setData(this, is);
        }
        finally
        {
            tree.removeGlobalBindings(generateBindingSupportId());
            is.close();
            file.close();

            if (removeAfterProcessing)
                file.remove();
        }
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }
}
