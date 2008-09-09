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

package org.raven.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import org.raven.ds.impl.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.collections.iterators.IteratorChain;
import org.raven.annotations.Parameter;
import org.raven.ds.DataConsumer;
import org.raven.ds.DataSource;
import org.raven.table.TableImpl;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.NodeAttributeImpl;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ScannerNode extends DataPipeImpl
{
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

    private Iterator<String> ipRangeIterator;
    private TableImpl ipTable;
    private String ip;
    private WorkerExecutor executor;
    private boolean dataSended;

    @Override
    public boolean start() throws NodeError
    {
        boolean started =  super.start();
        if (started)
            try {
                scan();
            } catch (Exception ex) {
                throw new NodeError(String.format("Error starting node (%s)", getPath()), ex);
            }
        return started;
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
    
    private void scan() throws Exception
    {
        dataSended = false;
        ipRangeIterator = formIpIterator();
        ipTable = new TableImpl(new String[]{"ip"});

        executor = new WorkerExecutor(getDataSource(), this);

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

    @Override
    protected void doSetData(DataSource dataSource, Object data)
    {
        if (dataSource==this)
            super.doSetData(dataSource, data);
    }

    private synchronized String getNextIp()
    {
        return ipRangeIterator.hasNext()? ipRangeIterator.next() : null;
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

    private synchronized void addIp(String ip)
    {
        ipTable.addRow(new Object[]{ip});
        if (logger.isDebugEnabled())
            logger.debug(String.format("Ip (%s) added to the table", ip));
    }

    private class WorkerExecutor
    {
        private final DataSource dataSource;
        private final DataConsumer dataConsumer;
        private final Thread[] workers;
        private final AtomicInteger activeWorkers;

        public WorkerExecutor(DataSource dataSource, DataConsumer dataConsumer)
        {
            this.dataSource = dataSource;
            this.dataConsumer = dataConsumer;

            int workersCount = threadCount;
            activeWorkers = new AtomicInteger(workersCount);
            workers = new Thread[workersCount];
            for (int i=0; i<workersCount; ++i)
            {
                Thread workerThread = new Thread(new Worker(dataSource, dataConsumer, this));
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
                setData((DataSource)dataConsumer, ipTable);
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

        public Worker(DataSource dataSource, DataConsumer dataConsumer, WorkerExecutor executor)
        {
            this.dataSource = dataSource;
            this.dataConsumer = dataConsumer;
            this.executor = executor;
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
                    if (dataSource.getDataImmediate(dataConsumer, sessAttribute))
                        addIp(ip);

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
