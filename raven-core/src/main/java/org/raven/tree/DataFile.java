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

package org.raven.tree;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 *
 * @author Mikhail Titov
 */
public interface DataFile extends AttributesGenerator
{
    /**
     * Returns the file name
     */
    public String getFilename() throws DataFileException;
    /**
     * Sets the file name
     */
    public void setFilename(String filename) throws DataFileException;
    /**
     * Returns the mime type of the file
     */
    public String getMimeType() throws DataFileException;
    /**
     * Sets the mime type
     */
    public void setMimeType(String mimeType) throws DataFileException;
    /**
     * Returns the data of the file
     */
    public InputStream getDataStream() throws DataFileException;
    /**
     * Returns content as reader if {@link #getEncoding() encoding} is set
     * @throws DataFileException 
     */
    public Reader getDataReader() throws DataFileException;
    /**
     * Sets the file data
     */
    public void setDataStream(InputStream data) throws DataFileException;
    /**
     * Sets data as string. Passed string will be encoded using charset which returns method {@link #getEncoding()}
     * or with default encoding if {@link #getEncoding()} return null
     * @throws DataFileException 
     */
    public void setDataString(String data) throws DataFileException;
    /**
     * Returns the file size in bytes
     */
    public Long getFileSize() throws DataFileException;
    /**
     * Returns the checksum of the file. Adler32 is used to calculate checksum
     */
    public Long getChecksum() throws DataFileException;
    /**
     * Returns the charset encoding for the content
     * @throws DataFileException 
     */
    public Charset getEncoding() throws DataFileException;
    /**
     * Sets charset with which content was encoded
     * @throws DataFileException 
     */
    public void setEncoding(Charset encoding) throws DataFileException;
}
