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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.activation.DataSource;
import org.apache.commons.io.FileUtils;
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
public class TemporaryFileManagerNode extends BaseNode implements Schedulable
{
    private final static int LOCK_WAIT_TIMEOUT = 500;

    @NotNull @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    private Scheduler scheduler;

    @NotNull @Parameter(defaultValue="raven_")
    private String tempFilePrefix;

    @NotNull @Parameter(defaultValue="5")
    private Integer timelife;

    @NotNull @Parameter
    private String directory;

    private Map<String, FileInfo> files;
    private ReadWriteLock lock;
    private File dirFile;

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
        if (!dirFile.exists() || !dirFile.isDirectory())
            throw new Exception(String.format("Directory (%s) not exists or not directory", directory));
    }

    public void executeScheduledJob(Scheduler scheduler)
    {
        if (!Status.STARTED.equals(getStatus()))
            return;
        try {
            if (lock.writeLock().tryLock(LOCK_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try{
                    Set<String> fileNames = new HashSet<String>();

                    //Deleting old files
                    long curtime = System.currentTimeMillis();
                    long _timelife = timelife*60*1000;
                    Iterator<FileInfo> it = files.values().iterator();
                    while (it.hasNext()) {
                        FileInfo fileInfo = it.next();
                        fileNames.add(fileInfo.file.getName());
                        if (curtime>fileInfo.time+_timelife) {
                            boolean hasError = false;
                            if (fileInfo.file.exists())
                                hasError = !fileInfo.file.delete();
                            if (!hasError)
                                it.remove();
                            else
                                fileInfo.deleteError = true;
                        }
                    }

                    //Deleting files that not in the files map.
                    FileUtils.iterateFiles(null, extensions, true)
                } finally {
                    lock.writeLock().unlock();
                }
            }else if (isLogLevelEnabled(LogLevel.TRACE))
                throw new InterruptedException();
        }catch(InterruptedException e){
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().debug("Write lock wait interrupted/timeout");
        }
    }

    @Override
    protected void initFields()
    {
        super.initFields();
        files = new LinkedHashMap<String, FileInfo>();
        lock = new ReentrantReadWriteLock();
    }

    public DataSource saveFile(String key, InputStream stream, boolean rewrite)
    {
        return null;
    }

    public DataSource get(String key)
    {
        
    }

    public void release(String key)
    {
        
    }

    private class FileInfo
    {
        private long time;
        private String key;
        private File file;
        private boolean deleteError = false;

        public FileInfo(long time, String key, File file) {
            this.time = time;
            this.key = key;
            this.file = file;
        }
    }
}
