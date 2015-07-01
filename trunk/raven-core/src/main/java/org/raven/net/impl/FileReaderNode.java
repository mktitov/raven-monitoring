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

import org.raven.tree.impl.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.Selectors;
import org.apache.commons.vfs.VFS;
import org.raven.annotations.NodeClass;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.impl.AbstractDataSource;
import org.raven.expr.BindingSupport;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.NodeAttribute;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class FileReaderNode extends AbstractDataSource
{
    public static String URL_ATTRIBUTE = "url";
    public static String FILEMASK_ATTRIBUTE = "fileMask";
    public static String REMOVEFILEAFTERPROCESSING_ATTRIBUTE = "removeFileAfterProcessing";
    
    @Message
    private static String urlDescription;

    @Message
    private static String fileMaskDescription;
    
    @Message
    private static String removeFileAfterProcessingDescription;

    @Override
    public boolean gatherDataForConsumer(DataConsumer dataConsumer, DataContext context)
        throws Exception
    {
        String url = context.getSessionAttributes().get(URL_ATTRIBUTE).getRealValue();
        String fileMask = context.getSessionAttributes().get(FILEMASK_ATTRIBUTE).getRealValue();
        Boolean removeAfterProcessing =
                context.getSessionAttributes().get(REMOVEFILEAFTERPROCESSING_ATTRIBUTE).getRealValue();

        FileSystemManager fsManager = VFS.getManager();

        FileObject baseFile = fsManager.resolveFile(url);
        List<FileObject> files = new ArrayList<FileObject>();
        if (baseFile.getType()==FileType.FILE)
            addFileForProcessing(files, baseFile);
        else
        {
            FileObject[] childs = baseFile.findFiles(Selectors.SELECT_FILES);
            Pattern mask = null;
            if (fileMask!=null && fileMask.trim().length()>0)
                mask = Pattern.compile(fileMask);
            
            if (childs!=null && childs.length!=0)
                for (FileObject fileObject: childs)
                    if (mask==null || mask.matcher(fileObject.getName().getBaseName()).matches())
                        addFileForProcessing(files, fileObject);
                    else
                    {
                        if (isLogLevelEnabled(LogLevel.DEBUG))
                            debug(String.format(
                                    "Ignoring file (%s). Not matches to file mask (%s)"
                                    , fileObject.getName().getBaseName(), fileMask));
                    }
                        
        }
        if (files.size()>0)
            for (FileObject file: files)
                try
                {
                    processFile(dataConsumer, file, removeAfterProcessing, context);
                }
                catch(Throwable e)
                {
                    error(String.format(
                            "Error in node (%s). Error processing file (%s). %s"
                            , getPath(), file, e.getMessage()), e);
                }
        
        return true;
    }

    private void addFileForProcessing(List<FileObject> files, FileObject file)
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("File (%s) added for processing", file.getName().getBaseName()));
        files.add(file);
    }

    @Override
    public void fillConsumerAttributes(Collection<NodeAttribute> consumerAttributes)
    {
        NodeAttribute attr = new NodeAttributeImpl(
                URL_ATTRIBUTE, String.class, null, urlDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(FILEMASK_ATTRIBUTE, String.class, null, fileMaskDescription);
        consumerAttributes.add(attr);

        attr = new NodeAttributeImpl(
                REMOVEFILEAFTERPROCESSING_ATTRIBUTE, Boolean.class, false
                , removeFileAfterProcessingDescription);
        attr.setRequired(true);
        consumerAttributes.add(attr);
    }

    private void processFile(
            DataConsumer dataConsumer, FileObject file, boolean removeAfterProcessing
            , DataContext context)
        throws Exception
    {
        if (isLogLevelEnabled(LogLevel.DEBUG))
            debug(String.format("Proccessing file (%s)", file.getName().getBaseName()));
        InputStream is = file.getContent().getInputStream();
        boolean fileProcessed = false;
        BindingSupport bindingSupport = new BindingSupportImpl();
        bindingSupport.put("fileName", file.getName().getBaseName());
        String bindingsId = generateBindingSupportId();
        tree.addGlobalBindings(bindingsId, bindingSupport);
        try
        {
            dataConsumer.setData(this, is, context);
            fileProcessed = true;
        }
        finally
        {
            tree.removeGlobalBindings(bindingsId);
            is.close();
            file.close();
			file.getFileSystem().getFileSystemManager().getFilesCache().removeFile(
					file.getFileSystem(), file.getName());

            if (fileProcessed && removeAfterProcessing)
                file.delete();
        }
    }
}
