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
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=TemporaryFileManagersNode.class)
public class TemporaryFileManagerNode extends BaseNode implements TemporaryFileManager, Schedulable
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
        try {
            executeScheduledJob(null);
        } finally {
            stopping.set(false);
        }
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
        if (!jobRunning.compareAndSet(false, true))
            return;
        try {
            if (!isStarted())
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
                        long _lifetime = timelife*1000;
                        Iterator<FileInfo> it = files.values().iterator();
                        while (it.hasNext()) {
                            FileInfo fileInfo = it.next();
                            fileNames.add(fileInfo.file.getName());
                            if (curtime>fileInfo.lastUsageTime+_lifetime || stopping.get()) {
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
                            if (!FileUtils.deleteQuietly(file) && isLogLevelEnabled(LogLevel.WARN))
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

    public File createFile(Node requester, String key, String contentType) throws IOException {
        return createFile(requester, key, contentType, null);
    }
    
    public File createFile(Node requester, String key, String contentType, String filename) throws IOException {
        lock.writeLock().lock();
        File file = null;
        try {
            FileInfo fileInfo = files.remove(key);
            file = File.createTempFile(tempFilePrefix, ".tmp", dirFile);
            if (fileInfo!=null)
                streamsToClose.addAll(fileInfo.streams);
            fileInfo = new FileInfo(requester, System.currentTimeMillis(), key, file, contentType, filename);
            files.put(key, fileInfo);
            fileInfo.initialized.set(true);
        } finally {
            lock.writeLock().unlock();
        }
        return file;
    }
    
    public File createDir(Node requester, String key, String contentType) throws IOException {
        lock.writeLock().lock();
        File file = null;
        try {
            FileInfo fileInfo = files.remove(key);
            file = File.createTempFile(tempFilePrefix, ".tmp", dirFile);
            if (fileInfo!=null)
                streamsToClose.addAll(fileInfo.streams);
            if (!file.delete() || !file.mkdir())
                throw new IOException("Can't create temporary directory");
            fileInfo = new FileInfo(requester, System.currentTimeMillis(), key, file, contentType, null);
            files.put(key, fileInfo);
            fileInfo.initialized.set(true);
        } finally {
            lock.writeLock().unlock();
        }
        return file;
    }
    
    public DataSource saveFile(Node creator, String key, InputStream stream, String contentType, boolean rewrite) 
        throws IOException
    {
        return saveFile(creator, key, stream, contentType, rewrite, null);
    }
    
    public DataSource saveFile(Node creator, String key, InputStream stream, String contentType, boolean 
            rewrite, String filename)
        throws IOException
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
            fileInfo = new FileInfo(
                    creator, System.currentTimeMillis(), key, tempFile, contentType, filename);
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

    public DataSource getDataSource(String key) {
        FileInfo fileInfo = getFileInfo(key);
        return fileInfo==null? null :  new TempDataSource(key);
    }
    
    public File getFile(String key) {
        FileInfo fileInfo = getFileInfo(key);
        return fileInfo!=null? fileInfo.file : null;
    }
    
    private FileInfo getFileInfo(String key) {
        lock.readLock().lock();
        try {
            final FileInfo fileInfo = files.get(key);
            if (fileInfo!=null && fileInfo.initialized.get()) {
                fileInfo.refreshUsageTime();
                return fileInfo;
            }                
            return null;
        } finally {
            lock.readLock().unlock();
        }
        
    }

    public void releaseDataSource(String key)
    {
        lock.writeLock().lock();
        try {
            files.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static class FileInfo
    {
        private final Node creator;
        private final long time;
        private final String key;
        private final File file;
        private final String filename;
        private final String contentType;
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final List<InputStream> streams = new LinkedList<InputStream>();
        private volatile long lastUsageTime;

        public FileInfo(Node creator, long time, String key, File file, String contentType, String filename) {
            this.time = time;
            this.lastUsageTime = time;
            this.key = key;
            this.file = file;
            this.creator = creator;
            this.contentType = contentType;
            this.filename = filename;
        }
        
        public void refreshUsageTime() {
            lastUsageTime = System.currentTimeMillis();
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
                fileInfo.refreshUsageTime();
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
            lock.readLock().lock();
            try {
                FileInfo fileInfo = files.get(key);                
                return fileInfo==null? null : fileInfo.contentType;
            } finally {
                lock.readLock().unlock();
            }
        }

        public String getName() 
        {
            lock.readLock().lock();
            try {
                FileInfo fileInfo = files.get(key);
                if (fileInfo==null) return null;
                else 
                    return fileInfo.filename!=null? fileInfo.filename : fileInfo.file.getAbsolutePath();
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
