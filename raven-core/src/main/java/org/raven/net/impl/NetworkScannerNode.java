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

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import org.raven.ds.impl.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.sched.Schedulable;
import org.raven.sched.Scheduler;
import org.raven.sched.impl.SystemSchedulerValueHandlerFactory;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.DataSourcesNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.ViewableObjectImpl;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=DataSourcesNode.class)
public class NetworkScannerNode extends DataPipeImpl implements Viewable, Schedulable
{
    @Parameter(valueHandlerType=SystemSchedulerValueHandlerFactory.TYPE)
    @NotNull()
    private Scheduler scheduler;

    @Parameter(defaultValue="5")
    @NotNull
    private Integer threadCount;

    @Parameter()
    @NotNull
    private String ipRanges;

    @Parameter()
    @NotNull
    private Integer interval;

    @Parameter()
    @NotNull
    private TimeUnit intervalUnit;

    @Parameter(defaultValue="true")
    @NotNull()
    private Boolean ipAddressFilter;

    @Parameter(defaultValue="host")
    @NotNull()
    private String hostAttributeName;

    @Parameter(defaultValue="true")
    @NotNull()
    private Boolean resolveHostnames;

    @Parameter(defaultValue="5")
    @NotNull()
    private Integer resolveNameRetryCount;

    private Iterator<String> ipRangeIterator;
    private TableImpl ipTable;
    private String ip;
    private WorkerExecutor executor;
    private AtomicLong scanStartTime;
    private AtomicLong scanEndTime;
    private boolean resolveHostnamesCopy;
    @Parameter(readOnly=true)
    private AtomicLong ipsScanned;
    @Parameter(readOnly=true)
    private AtomicLong ipsFound;

    private Lock lock = new ReentrantLock();

    public void executeScheduledJob(Scheduler scheduler)
    {
        try {
            scan();
        } catch (Exception ex) {
            throw new NodeError(String.format("Error starting node (%s)", getPath()), ex);
        }
    }

    @Override
    public synchronized void stop() throws NodeError
    {
        if (executor!=null && !executor.isJobDone())
        {
            try {
                TimeUnit.SECONDS.sleep(1);
                executor.shutdown();
            } catch (InterruptedException ex) {
                throw new NodeError(String.format("Error stoping node (%s)", getPath()), ex);
            }
        }
        super.stop();
    }

    @Override
    public boolean getDataImmediate(DataConsumer dataConsumer, DataContext context)
    {
        if (ipTable!=null && executor.isJobDone())
            setData(this, ipTable, context);
        else
            setData(this, null, context);
        return true;
    }
    
    private void scan() throws Exception
    {
        if (lock.tryLock())
        {
            try
            {
                ipRangeIterator = formIpIterator();
                resolveHostnamesCopy = resolveHostnames;
                if (resolveHostnamesCopy)
                    ipTable = new TableImpl(new String[]{"ip", "hostname"});
                else
                    ipTable = new TableImpl(new String[]{"ip"});

                scanStartTime = new AtomicLong(System.currentTimeMillis());
                scanEndTime = new AtomicLong();
                ipsScanned = new AtomicLong();
                ipsFound = new AtomicLong();
                executor = new WorkerExecutor(getDataSource(), this);
            }finally
            {
                lock.unlock();
            }
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Parameter(readOnly=true)
    public boolean isScanning()
    {
        return executor!=null && !executor.isJobDone();
    }

    @Parameter(readOnly=true)
    public String getScanningDuration()
    {
        long dur = getScanDurationInMillisec();
        return dur==0? "" : DurationFormatUtils.formatDuration(dur, "H:m:s.S", true);
    }

    @Parameter(readOnly=true)
    public String getScanningStartTime()
    {
        long startTime = scanStartTime==null? 0l : scanStartTime.get();
        if (startTime==0)
            return "";
        else
        {
            SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            return fmt.format(new Date(startTime));
        }
    }

    public AtomicLong getIpsScanned()
    {
        return ipsScanned;
    }

    public AtomicLong getIpsFound()
    {
        return ipsFound;
    }

    @Parameter(readOnly=true)
    public Long getScanningSpeed()
    {
        if (scanStartTime==null || scanStartTime.get()==0)
            return 0l;
        long dur = getScanDurationInMillisec();
        return dur==0? 0 : ipsScanned.get()/(dur/1000);
    }

    public Integer getResolveNameRetryCount() {
        return resolveNameRetryCount;
    }

    public void setResolveNameRetryCount(Integer resolveNameRetryCount) {
        this.resolveNameRetryCount = resolveNameRetryCount;
    }

    public String getHostAttributeName() {
        return hostAttributeName;
    }

    public void setHostAttributeName(String hostAttributeName) {
        this.hostAttributeName = hostAttributeName;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public TimeUnit getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(TimeUnit intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public Boolean getIpAddressFilter() {
        return ipAddressFilter;
    }

    public void setIpAddressFilter(Boolean ipAddressFilter) {
        this.ipAddressFilter = ipAddressFilter;
    }

    public String getIpRanges() {
        return ipRanges;
    }

    public void setIpRanges(String ipRanges) {
        this.ipRanges = ipRanges;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public Boolean getResolveHostnames() {
        return resolveHostnames;
    }

    public void setResolveHostnames(Boolean resolveHostnames) {
        this.resolveHostnames = resolveHostnames;
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context)
    {
        if (dataSource==this)
            super.doSetData(dataSource, data, context);
    }
    
    private long getScanDurationInMillisec()
    {
        long time = 0;
        long startTime = scanStartTime==null? 0l : scanStartTime.get();
        if (startTime>=0)
        {
            if (scanEndTime==null || scanEndTime.get()==0)
                time = System.currentTimeMillis();
            else
                time = scanEndTime.get();
        }
        return startTime==0? 0 : time-startTime;
    }

    private synchronized String getNextIp()
    {
        if (ipRangeIterator.hasNext())
        {
            ipsScanned.incrementAndGet();
            return ipRangeIterator.next();
        }
        else
            return null;
    }

    private Collection<NodeAttribute> getSessionAttributes(String ip) throws Exception
    {
        NodeAttribute hostAttr = new NodeAttributeImpl(hostAttributeName, String.class, ip, null);
        hostAttr.setOwner(this);
        hostAttr.init();

        return Arrays.asList(hostAttr);
    }

    private Iterator<String> formIpIterator() throws Exception
    {
        String[] ranges = ipRanges.split("\\s*,\\s*");
        List<Iterator<String>> ipRangesList = new ArrayList<Iterator<String>>();
        for (String range: ranges)
        {
            String[] ips = range.split("\\s*-\\s*");
            if (ips==null || ips.length!=2)
                throw new Exception(String.format("Invalid ip range (%s)", range));
            ipRangesList.add(new IpRange(ips[0], ips[1]).getIterator());
        }
        if (ipRangesList.size()==0)
            throw new Exception(String.format("Empty ip ranges expression (%s)", ipRanges));
        
        return new IteratorChain(ipRangesList);
    }

    private synchronized boolean checkIp(String ip)
    {
        this.ip = ip;
        return ipAddressFilter;
    }

    @Override
    public void formExpressionBindings(Bindings bindings)
    {
        super.formExpressionBindings(bindings);
        bindings.put("ip", ip);
    }

    private synchronized void addIp(String ip, String hostname)
    {
        if (resolveHostnamesCopy)
            ipTable.addRow(new Object[]{ip, hostname});
        else
            ipTable.addRow(new Object[]{ip});
        ipsFound.incrementAndGet();
        if (logger.isDebugEnabled())
            logger.debug(String.format("Ip (%s) added to the table", ip));
    }

    @Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception
    {
        return null;
    }

    @Override
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) 
            throws Exception
    {
        if (ipTable==null)
            return null;
        ViewableObject obj = new ViewableObjectImpl(Viewable.RAVEN_TABLE_MIMETYPE, ipTable);
        
        return Arrays.asList(obj);
    }

    private class WorkerExecutor
    {
        private final DataSource dataSource;
        private final DataConsumer dataConsumer;
        private final Thread[] workers;
        private final AtomicInteger activeWorkers;
        private final int retryCount = resolveNameRetryCount;

        public WorkerExecutor(DataSource dataSource, DataConsumer dataConsumer)
        {
            this.dataSource = dataSource;
            this.dataConsumer = dataConsumer;

            int workersCount = threadCount;
            activeWorkers = new AtomicInteger(workersCount);
            workers = new Thread[workersCount];
            for (int i=0; i<workersCount; ++i)
            {
                Thread workerThread =
                        new Thread(new Worker(dataSource, dataConsumer, this, retryCount));
                workers[i] = workerThread;
                workerThread.start();
            }
        }

        public void oneJobDone()
        {
            int count = activeWorkers.decrementAndGet();
            if (logger.isDebugEnabled())
                logger.debug("Active workers count - "+count);
            if (count==0)
            {
                scanEndTime.set(System.currentTimeMillis());
                setData((DataSource)dataConsumer, ipTable, new DataContextImpl());
            }
        }

        public boolean isJobDone()
        {
            return activeWorkers.get()<=0;
        }

        public void shutdown()
        {
            for (int i=0; i<workers.length; ++i)
                if (workers[i].isAlive())
                    workers[i].interrupt();
        }

    }

    private class Worker implements Runnable
    {
        private final DataSource dataSource;
        private final DataConsumer dataConsumer;
        private final WorkerExecutor executor;
        private final int retryCount;

        public Worker(
                DataSource dataSource, DataConsumer dataConsumer, WorkerExecutor executor
                , int retryCount)
        {
            this.dataSource = dataSource;
            this.dataConsumer = dataConsumer;
            this.executor = executor;
            this.retryCount = retryCount;
        }

        public void run()
        {
            String ip = getNextIp();
            while (getStatus()==Status.STARTED && ip!=null && checkIp(ip) )
            {
                if (logger.isDebugEnabled())
                    logger.debug("Scanning ip "+ip);
                try
                {
                    Collection<NodeAttribute> sessAttribute = getSessionAttributes(ip);
                    if (dataSource.getDataImmediate(dataConsumer, new DataContextImpl(sessAttribute)))
                    {
                        if (resolveHostnamesCopy)
                        {
                            InetAddress addr = InetAddress.getByName(ip);
                            String hostname = null;
                            for (int i=0; i<retryCount+1; ++i)
                            {
                                hostname = addr.getCanonicalHostName();
                                if (!ip.equals(hostname))
                                    break;
                                TimeUnit.MILLISECONDS.sleep(10);
                            }
                            addIp(ip, hostname);
                        }
                        else
                            addIp(ip, null);
                    }

                    TimeUnit.MILLISECONDS.sleep(1);
                    ip = getNextIp();
                }catch (Exception e)
                {
                    getLogger().error(
                            String.format("Error scanning ip (%s). %s", ip, e.getMessage()), e);
                }
            }
            executor.oneJobDone();
        }
    }
}
