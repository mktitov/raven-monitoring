/*
 * Copyright 2016 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.net;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * The communication mechanism with low level http response
 * @author Mikhail Titov
 */
public interface ResponseAdapter {
    /**
     * Returns the stream of the response channel
     */
    public OutputStream getStream();
    /**
     * Returns the writer for the response channel
     */
    public PrintWriter getWriter() throws IOException;
    /**
     * Closes the response channel
     */
    public void close();
    /**
     * Adds a header to low level http response
     * @param name the name of the header
     * @param value the value of the header
     */    
    public void addHeader(String name, String value);
}
