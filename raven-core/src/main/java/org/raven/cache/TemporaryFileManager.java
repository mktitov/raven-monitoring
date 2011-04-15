/*
 *  Copyright 2011 Mikhail Titov.
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

package org.raven.cache;

import java.io.IOException;
import java.io.InputStream;
import javax.activation.DataSource;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface TemporaryFileManager extends Node
{
    /**
     * Stores inputStream to the temporary file and returns the DataSource for created file
     * @param requester the node that creates the temporary file
     * @param key the unique key for this temporary file
     * @param stream the stream from which temporary file will created
     * @param contentType the mime type of the stream content
     * @param rewrite if true and manager have the temporary file with the same key, then the
     *      existent file will be rewrote by this one. If false but manager contains the temporary
     *      file with the same key, then the method returns the data source for the existent file.
     * @throws IOException
     */
    public DataSource saveFile(Node requester, String key, InputStream stream, String contentType
            , boolean rewrite)
        throws IOException;
    /**
     * Returns the data source for key passed in the parameter or null if manager does not contain
     * the temporary file for the given key.
     * @param key of the previously created temporary file
     * @see #saveFile
     */
    public DataSource getDataSource(String key);
    /**
     * Delete temporary for the given key from the manager
     */
    public void releaseDataSource(String key);
}
