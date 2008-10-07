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

import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.tree.NodeAttribute;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class SmbFileReader extends AbstractFileReader
{
    @Override
    protected String getUrlDescription()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected FileWrapper resolveFile(String url, Map<String, NodeAttribute> attrs)
            throws FileReaderException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected FileWrapper[] getChildrens(
            FileWrapper file, FilenameFilter filenameFilter, Map<String, NodeAttribute> attributes)
        throws FileReaderException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
