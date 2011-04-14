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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.activation.DataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.log.LogLevel;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class TemporaryFileManagerNode extends BaseNode implements Schedulable
{
    private final static int LOCK_WAIT_TIMEOUT = 500;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler scheduler;

    @NotNull @Parameter(defaultValue="raven_")
    private String tempFilePrefix;

    @NotNull @Parameter(defaultValue="300")
    private Integer timelife;

    @NotNull @Parameter
    private String directory;

    @NotNull @Parameter(defaultValue="false")
    private Boolean forceCreateDirectory;

    private Map<String, FileInfo> files;
    private Collection<InputStream> streamsToClose;
    private ReadWriteLock lock;
    private File dirFile;
    private AtomicBoolean jobRunning;
    private AtomicBoolean stopping;

    public String getTempFilePrefix() {
        return tempFilePrefix;
    }

    public void setTempFilePrefix(String tempFilePrefix) {
        this.tempFilePrefix = tempFilePrefix;
    }

    public Boolean getForceCreateDirectory() {
        return forceCreateDirectory;
    }

    public void setForceCreateDirectory(Boolean forceCreateDirectory) {
        this.forceCreateDirectory = forceCreateDirectory;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Integer getTimelife() {
        return timelife;
    }

    public void setTimelife(Integer timelife) {
        this.timelife = timelife;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        dirFile = new File(directory);
        if (forceCreateDirectory && !dirFile.exists())
            dirFile.mkdirs();
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            throw new Exception(String.format(
                    "Directory (%s) not exists or not directory", directory));
        }
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        while (jobRunning.get())
            TimeUnit.MILLISECONDS.sleep(100);
        stopping.set(true);
        executeScheduledJob(null);
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
        if (!jobRunning.compareAndSet(false, true))
            return;
        try {
            if (!Status.STARTED.equals(getStatus()))
                return;
            try {
                if (lock.writeLock().tryLock(LOCK_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    Collection<File> filesToDelete = new LinkedList<File>();
                    Collection<InputStream> _streamsToClose = new LinkedList<InputStream>();
                    _streamsToClose.addAll(streamsToClose);
                    streamsToClose.clear();
                    try{
                        Set<String> fileNames = new HashSet<String>();

                        //Looking for old files
                        long curtime = System.currentTimeMillis();
                        long _timelife = timelife*1000;
                        Iterator<FileInfo> it = files.values().iterator();
                        while (it.hasNext()) {
                            FileInfo fileInfo = it.next();
                            fileNames.add(fileInfo.file.getName());
                            if (curtime>fileInfo.time+_timelife || stopping.get()) {
                                it.remove();
                                filesToDelete.add(fileInfo.file);
                                synchronized(fileInfo){
                                    _streamsToClose.addAll(fileInfo.streams);
                                }
                            }
                        }

                        //Looking for temporary files that are not in files map.
                        Iterator<File> tempFiles = FileUtils.iterateFiles(
                                dirFile, new WildcardFileFilter(tempFilePrefix+"*.tmp"), null);
                        while (tempFiles.hasNext())
                        {
                            File file = tempFiles.next();
                            if (!fileNames.contains(file.getName()))
                                filesToDelete.add(file);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                    if (!filesToDelete.isEmpty())
                        for (File file: filesToDelete)
                            if (!file.delete() && isLogLevelEnabled(LogLevel.WARN))
                                getLogger().warn(
                                        "Can't delete old temporary file ({})"
                                        , file.getAbsolutePath());
                    if (!_streamsToClose.isEmpty())
                        for (InputStream stream: _streamsToClose)
                            IOUtils.closeQuietly(stream);
                    
                }else if (isLogLevelEnabled(LogLevel.TRACE))
                    throw new InterruptedException();
            }catch(InterruptedException e){
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().debug("Write lock wait interrupted/timeout");
            }
        } finally {
            jobRunning.set(false);
        }
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        files = new LinkedHashMap<String, FileInfo>();
        lock = new ReentrantReadWriteLock();
        jobRunning = new AtomicBoolean(Boolean.FALSE);
        stopping = new AtomicBoolean(false);
        streamsToClose = new LinkedList<InputStream>();
    }

    public DataSource saveFile(String key, InputStream stream, boolean rewrite) throws IOException
    {
        lock.writeLock().lock();
        FileInfo fileInfo = null;
        try {
            fileInfo = files.get(key);
            if (fileInfo!=null && !rewrite)
                return new TempDataSource(key);

            File tempFile = File.createTempFile(tempFilePrefix, ".tmp", dirFile);
            if (fileInfo!=null)
                streamsToClose.addAll(fileInfo.streams);
            fileInfo = new FileInfo(System.currentTimeMillis(), key, tempFile);
            files.put(key, fileInfo);
        } finally {
            lock.writeLock().unlock();
        }
        FileOutputStream out = null;
        try {
            try {
                out = new FileOutputStream(fileInfo.file);
                IOUtils.copy(stream, out);
            } catch (IOException e) {
                lock.writeLock().lock();
                try {
                    files.remove(key);
                } finally {
                    lock.writeLock().unlock();
                }
                throw e;
            }
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(out);
        }
        fileInfo.initialized.set(true);
        
        return new TempDataSource(key);
    }

    public DataSource getDataSource(String key)
    {
        lock.readLock().lock();
        try {
            FileInfo fileInfo = files.get(key);
            return fileInfo==null||!fileInfo.initialized.get()? null : new TempDataSource(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void release(String key)
    {
        lock.writeLock().lock();
        try {
            files.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private class FileInfo
    {
        private final long time;
        private final String key;
        private final File file;
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final List<InputStream> streams = new LinkedList<InputStream>();

        public FileInfo(long time, String key, File file) {
            this.time = time;
            this.key = key;
            this.file = file;
        }
    }

    private class TempDataSource implements DataSource
    {
        private final String key;

        public TempDataSource(String key)
        {
            this.key = key;
        }

        public InputStream getInputStream() throws IOException
        {
            lock.readLock().lock();
            try {
                FileInfo fileInfo = files.get(key);
                if (fileInfo==null)
                    throw new IOException("Temporary file (%s) unavailable");
                FileInputStream in = new FileInputStream(fileInfo.file);
                synchronized(fileInfo) {
                    fileInfo.streams.add(in);
                }
                return in;
            } finally {
                lock.readLock().unlock();
            }
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getContentType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getName() 
        {
            lock.readLock().lock();
            try {
                FileInfo fileInfo = files.get(key);
                return fileInfo==null? null : fileInfo.file.getAbsolutePath();
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
