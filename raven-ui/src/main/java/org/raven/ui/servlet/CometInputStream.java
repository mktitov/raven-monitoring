/*
 * Copyright 2014 tim.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Mikhail Titov
 */
public class CometInputStream extends InputStream {
    private final InputStream source;
    private final AtomicBoolean sourceClosed = new AtomicBoolean();

    public CometInputStream(InputStream sourceStream) {
        this.source = sourceStream;
    }
    
    public void sourceClosed() {
        if (sourceClosed.compareAndSet(false, true))
            synchronized(this) {
                notify();
            }
    }
    
    public void newDataAvailableInSource() {
        synchronized(this) {
            notify();
        }
    }

    @Override
    public int read() throws IOException {
        if (sourceClosed.get())
            return -1;
        final int data = source.read();
        if (data!=-1)
            return data;
        //wait for data in source
        try {
            while (!sourceClosed.get() && source.available()<=0) {
                synchronized(this) {
                    wait(100l);
                }
            }
            return sourceClosed.get()? -1 : source.read();            
        } catch (InterruptedException ex) {
            source.close();
            return -1;
        }
    }
    
}
