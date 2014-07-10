/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.ui.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.raven.tree.impl.LoggerHelper;

/**
 *
 * @author Mikhail Titov 
 */
public class CometReader extends Reader implements IncomingDataListener {
    private final Reader source;
    private final AtomicBoolean sourceClosed = new AtomicBoolean();

    public CometReader(Reader source) {
        this.source = source;
    }
    
    @Override
    public int read(char[] chars, int off, int len) throws IOException {
        if (sourceClosed.get())
            return -1;
        final int data = source.read();
        if (data!=-1)
            return data;
        //wait for data in source
        try {
            while (!sourceClosed.get() && !source.ready()) {
                synchronized(this) {
                    wait(100l);
                }
            }
            return source.read(chars, off, len);            
        } catch (InterruptedException ex) {
            return -1;
        }
        
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    public void newDataAvailable() {
        synchronized(this) {
            notify();
        }
    }

    public void dataStreamClosed() {
        if (sourceClosed.compareAndSet(false, true))
            synchronized(this) {
                notify();
            }
    }
}
