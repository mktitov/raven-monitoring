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

import java.util.Collection;
import java.util.Map;
import jcifs.smb.SmbFile;
import org.raven.annotations.NodeClass;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class SmbFileReader extends AbstractFileReader
{
    public static String SMBFILEMASK_ATTRIBUTE = "smbFileMaskDescription";

    @Message
    private static String urlDescription;

    @Message
    private static String smbFileMaskDescription;

    @Override
    protected String getUrlDescription()
    {
        return urlDescription;
    }

    @Override
    protected FileWrapper resolveFile(String url, Map<String, NodeAttribute> attrs)
            throws FileReaderException
    {
        try
        {
            SmbFile file = new SmbFile(url);
            if (!file.exists())
                throw new FileReaderException("File not exists");
            SmbFileWrapper fileWrapper = new SmbFileWrapper(file);

            return fileWrapper;
        }
        catch(Throwable e)
        {
            throw new FileReaderException(String.format(
                    "Error resolving file (%s). %s", url, e.getMessage()), e);
        }
    }

    @Override
    protected FileWrapper[] getChildrens(
            FileWrapper fileWrapper, Map<String, NodeAttribute> attributes)
        throws FileReaderException
    {
        try
        {
            NodeAttribute smbFileMaskAttr = attributes.get(SMBFILEMASK_ATTRIBUTE);
            String smbFileMask = null;
            if (smbFileMaskAttr!=null)
                smbFileMask = smbFileMaskAttr.getRealValue();

            SmbFile[] files = null;
            SmbFile smbFile = ((SmbFileWrapper)fileWrapper).getSmbFile();
            if (smbFileMask!=null)
                files = smbFile.listFiles(smbFileMask);
            else
                files = smbFile.listFiles();

            if (files!=null && files.length==0)
                return null;
            else
            {
                FileWrapper[] fileWrappers = new FileWrapper[files.length];
                for (int i=0; i<files.length; ++i)
                    fileWrappers[i] = new SmbFileWrapper(files[i]);
                
                return fileWrappers;
            }
        }
        catch(Throwable e)
        {
            if ("The system cannot find the file specified.".equals(e.getMessage()))
                return null;
            throw new FileReaderException(String.format(
                    "Error reading files from directory (%s). %s"
                        , fileWrapper.getName(), e.getMessage())
                    , e);
        }
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        super.fillConsumerAttributes(consumerAttributes);

        NodeAttributeImpl attr = new NodeAttributeImpl(
                SMBFILEMASK_ATTRIBUTE, String.class, null, smbFileMaskDescription);
        attr.setRequired(false);
        consumerAttributes.add(attr);
    }


}
