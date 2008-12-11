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

package org.raven.net;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.raven.ds.DataConsumer;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractFileReader extends AbstractDataSource
{
    public static String URL_ATTRIBUTE = "url";
    public static String REGEXP_FILEMASK_ATTRIBUTE = "regexpFileMask";
    public static String REMOVEFILEAFTERPROCESSING_ATTRIBUTE = "removeFileAfterProcessing";
    public static String ADDFILENAMETOSTREAM_ATTRIBUTE = "addFilenameToStream";
    
    @Message
    private static String regexpFileMaskDescription;
    
    @Message
    private static String removeFileAfterProcessingDescription;
    
    @Message
    private static String addFilenameToStreamDescription;

    @Override
    public boolean gatherDataForConsumer(
            DataConsumer dataConsumer, Map<String, NodeAttribute> attributes) throws Exception
    {
        String url = attributes.get(URL_ATTRIBUTE).getRealValue();
        String fileMask = attributes.get(REGEXP_FILEMASK_ATTRIBUTE).getRealValue();
        Boolean removeAfterProcessing =
                attributes.get(REMOVEFILEAFTERPROCESSING_ATTRIBUTE).getRealValue();
//        Boolean addFilenameToStream = attributes.get(ADDFILENAMETOSTREAM_ATTRIBUTE).getRealValue();
        boolean addFilenameToStream = false;

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
                                dataConsumer, fileWrapper, addFilenameToStream
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
            if (logger.isDebugEnabled())
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

        attr = new NodeAttributeImpl(
                REGEXP_FILEMASK_ATTRIBUTE, String.class, null, regexpFileMaskDescription);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                REMOVEFILEAFTERPROCESSING_ATTRIBUTE, Boolean.class, false
                , removeFileAfterProcessingDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

//        attr = new NodeAttributeImpl(
//                ADDFILENAMETOSTREAM_ATTRIBUTE, Boolean.class, false
//                , addFilenameToStreamDescription);
//        attr.setRequired(true);
//        consumerAttributes.add(attr);
    }

    protected abstract String getUrlDescription();

    protected abstract FileWrapper resolveFile(String url, Map<String, NodeAttribute> attrs)
        throws FileReaderException;

    protected abstract FileWrapper[] getChildrens(
            FileWrapper file, Map<String, NodeAttribute> attributes)
        throws FileReaderException;

    private void processFile(
            DataConsumer dataConsumer, FileWrapper file,
            Boolean addFilenameToStream, Boolean removeAfterProcessing)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug(String.format("Proccessing file (%s)", file.getName()));
        InputStream is = file.getInputStream();
        if (addFilenameToStream)
        {
            
        }
        try
        {
            dataConsumer.setData(this, is);
        }
        finally
        {
            is.close();
            file.close();

            if (removeAfterProcessing)
                file.remove();
        }
    }
}
