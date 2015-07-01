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

import java.io.InputStream;

/**
 *
 * @author Mikhail Titov
 */
public interface FileWrapper
{
    public enum FileType {FILE, DIRECTORY};
    /**
     * Returns the base name of the file. The file name without path.
     */
    public String getName();
    /**
     * Returns the file type.
     */
    public FileType getType();
    /**
     * Returns the input stream of the file 
     */
    public InputStream getInputStream() throws Exception;
    /**
     * Closes the file
     */
    public void close() throws Exception;
    /**
     * Removes the file.
     */
    public void remove() throws Exception;
    /**
     * Returns the children files of this file.
     */
//    public FileWrapper[] getChildrens();
}
