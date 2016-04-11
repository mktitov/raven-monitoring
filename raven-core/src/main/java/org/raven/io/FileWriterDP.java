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
package org.raven.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.AbstractDataProcessorLogic;
import org.raven.dp.impl.Behaviour;

/**
 *
 * @author Mikhail Titov
 */
public class FileWriterDP extends AbstractDataProcessorLogic {
    public final static String EOF = "EOF";
    public final static String CLOSED = "CLOSED";
    public final static String ERROR = "ERROR";
    public final static String OPEN = "OPEN";
    public final static String OPENED = "OPENED";
    private final static String WRITE_COMPLETED = "WRITE_COMPLETED";
    private final WriteCompletionHandler writeCompletionHandler;
    
    private final DataProcessorFacade dataSource;
    private final File file;    
    private final ByteBuffer writeBuffer;
    private ByteBuffer writingBuffer;
    private AsynchronousFileChannel fileChannel;
    private long pos;
    
    private final Behaviour eofBehaviour = new Behaviour("CLOSE") {        
        @Override public Object processData(Object message) throws Exception {
            if (message=="EOF") {
                getFacade().sendTo(dataSource, CLOSED);
                getFacade().stop();
                return VOID;
            } else
                return UNHANDLED;
        }
    };
    
    private final Behaviour readyStage = new Behaviour("READY") {        
        @Override public Object processData(Object message) throws Exception {
            if (message instanceof ByteBuffer) {
                writingBuffer = (ByteBuffer) message;
                fileChannel.write(writeBuffer, pos, null, writeCompletionHandler);
                become(writingStage);
                return VOID;
            } else
                return UNHANDLED;
        }
    }.andThen(eofBehaviour);
    
    private final Behaviour writingStage = new Behaviour("WRITING") {        
        @Override public Object processData(Object message) throws Exception {
            if (message==WRITE_COMPLETED) {                
                getContext().unstashAll();
                become(readyStage);
            } else if (message instanceof Throwable) {
                if (getLogger().isErrorEnabled())
                    getLogger().error("Error writing to file");
                getFacade().sendTo(dataSource, ERROR);
                getFacade().stop();
            } else 
                getContext().stash();                
            return VOID;
        }
    };

    public FileWriterDP(DataProcessorFacade dataSource, File file, ByteBuffer writeBuffer) {
        this.dataSource = dataSource;
        this.file = file;
        this.writeBuffer = writeBuffer;
        this.writeCompletionHandler = new WriteCompletionHandler();
        this.pos = 0;
    }

    @Override
    public void postStop() {
        if (getLogger().isDebugEnabled()) 
            getLogger().debug("Closing file channel");
        if (fileChannel!=null) {
            try {
                fileChannel.close();
            } catch (IOException ex) {
                if (getLogger().isErrorEnabled())
                    getLogger().error(String.format("Error closing file channel (%s)", file), ex);
            }
        }
    }
    
        
    @Override
    public Object processData(Object message) throws Exception {
        if (message==OPEN) {
            openFile();
            getFacade().sendTo(dataSource, OPENED);
            return VOID;
        } else
            return UNHANDLED;
    }
    
    private void openFile() {
        try {
            fileChannel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            become(readyStage);
        } catch (IOException ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Error opening file channel", ex);
            }
            getFacade().sendTo(dataSource, ERROR);
            getFacade().stop();
        }
    }
    
    private class WriteCompletionHandler implements CompletionHandler<Integer, Object> {
        
        @Override
        public void completed(Integer result, Object attachment) {
            getFacade().send(WRITE_COMPLETED);
        }

        @Override
        public void failed(Throwable error, Object attachment) {
            getFacade().send(error);
        }
    }
}
