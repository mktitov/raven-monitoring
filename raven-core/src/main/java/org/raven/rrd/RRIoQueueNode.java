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

package org.raven.rrd;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.rrd.data.RRDNode;
import org.raven.rrd.data.RRDataSource;
import org.raven.tree.NodeError;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.QueuesNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=QueuesNode.class)
public class RRIoQueueNode extends BaseNode
{
	@Parameter(defaultValue="2")
	@NotNull
	private Integer corePoolSize;

	@Parameter(defaultValue="6")
	@NotNull
	private Integer maximumPoolSize;

	@Parameter(defaultValue="60")
	@NotNull
	private Long keepAliveTime;

	@Parameter(defaultValue="SECONDS")
	@NotNull
	private TimeUnit timeUnit;

	@Parameter(readOnly=true)
	private AtomicInteger maximumQueueSize;

	private ThreadPoolExecutor executor;
	private Lock pushLock;
	private Lock dbFlagsLock;
	private Lock avgWriteTimeLock;
	private Lock avgRequestsLock;
	private Set<Integer> dbFlags;
	private long writeTimeSum;
	private long writesCount;
	private long requestsCount;
	private long nodeStartTime;

	@Override
	protected void initFields()
	{
		super.initFields();

		pushLock = new ReentrantLock();
		dbFlags = new HashSet<Integer>();
		avgWriteTimeLock = new ReentrantLock();
		avgRequestsLock = new ReentrantLock();

		resetStatisticFields();
	}

	private void resetStatisticFields()
	{
		writeTimeSum = 0;
		writesCount = 0;
		requestsCount = 0;
		nodeStartTime = 0;
	}

	@Override
	protected void doStart() throws Exception
	{
		super.doStart();

		nodeStartTime = System.currentTimeMillis();

		executor = new ThreadPoolExecutor(
			corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, new LinkedBlockingQueue());
	}

	@Override
	public synchronized void stop() throws NodeError
	{
		super.stop();

		executor.shutdown();
		executor = null;

		resetStatisticFields();
	}

	public void pushWriteRequest(RRDataSource dataSource, Object data)
	{
		if (getStatus()!=Status.STARTED)
		{
			error(String.format(
				"Error handling write request for rrd datasource (%s). Node (%s) not started"
				, dataSource.getPath(), getPath()));
			return;

		}
		pushLock.lock();
		try
		{
			int queueSize = executor.getQueue().size();
			if (maximumQueueSize==null)
				maximumQueueSize = new AtomicInteger(queueSize);
			else if (maximumQueueSize.get()>queueSize)
				maximumQueueSize.set(queueSize);
		}
		finally
		{
			pushLock.unlock();
		}

		avgRequestsLock.lock();
		try
		{
			requestsCount++;
		}
		finally
		{
			avgRequestsLock.unlock();
		}

		executor.execute(new WriteRequest(dataSource, data));
	}

	public Integer getCorePoolSize()
	{
		return corePoolSize;
	}

	public void setCorePoolSize(Integer corePoolSize)
	{
		this.corePoolSize = corePoolSize;
	}

	public Integer getMaximumPoolSize()
	{
		return maximumPoolSize;
	}

	public void setMaximumPoolSize(Integer maximumPoolSize)
	{
		this.maximumPoolSize = maximumPoolSize;
	}

	public Long getKeepAliveTime()
	{
		return keepAliveTime;
	}

	public void setKeepAliveTime(Long keepAliveTime)
	{
		this.keepAliveTime = keepAliveTime;
	}

	public TimeUnit getTimeUnit()
	{
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit)
	{
		this.timeUnit = timeUnit;
	}

	public AtomicInteger getMaximumQueueSize()
	{
		return maximumQueueSize;
	}

	public void setMaximumQueueSize(AtomicInteger maximumQueueSize)
	{
		this.maximumQueueSize = maximumQueueSize;
	}

	@Parameter(readOnly=true)
	public Integer getAverageWriteTime()
	{
		avgWriteTimeLock.lock();
		try
		{
			return writesCount==0? null : (int)(writeTimeSum/writesCount);
		}
		finally
		{
			avgWriteTimeLock.unlock();
		}
	}

	@Parameter(readOnly=true)
	public Double getAverageWritesPerSecond()
	{
		Integer avgTime = getAverageWriteTime();
		
		return avgTime==null? null : 1000./avgTime.doubleValue();
	}

	@Parameter(readOnly=true)
	public Double getAverageRequestsPerSecond()
	{
		return requestsCount==0? null : requestsCount/(nodeStartTime/1000.);
	}
	
    @Parameter(readOnly=true)
    public Long getRequestsCount()
    {
        return requestsCount==0? null : requestsCount;
    }

	@Parameter(readOnly=true)
	public Integer getQueueSize()
	{
		return getStatus()==Status.STARTED? executor.getQueue().size() : null;
	}

    @Parameter(readOnly=true)
    public Integer getActiveThreads()
    {
        return Status.STARTED!=getStatus()? 0 : executor.getActiveCount();
    }

    @Parameter(readOnly=true)
    public Integer getThreadsInPool()
    {
        return Status.STARTED!=getStatus()? 0 : executor.getPoolSize();
    }

    @Parameter(readOnly=true)
    public Integer getLargestPoolSize()
    {
        return Status.STARTED!=getStatus()? 0: executor.getLargestPoolSize();
    }

	private void calculateAvgWriteTime(long requestHandleTime)
	{
		avgWriteTimeLock.lock();
		try
		{
			writeTimeSum += requestHandleTime;
			writesCount++;
		}
		finally
		{
			avgWriteTimeLock.unlock();
		}
	}

	private class WriteRequest implements Runnable
	{
		private final RRDataSource dataSource;
		private final Object data;

		public WriteRequest(RRDataSource dataSource, Object data)
		{
			this.dataSource = dataSource;
			this.data = data;
		}

		public void run()
		{
			long startTime = System.currentTimeMillis();
			RRDNode rrd = (RRDNode) dataSource.getEffectiveParent();
			int rrdId = rrd.getId();
			dbFlagsLock.lock();
			try
			{
				if (dbFlags.contains(rrdId))
				{
					pushWriteRequest(dataSource, data);
					return;
				}
				else
					dbFlags.add(rrdId);
			}
			finally
			{
				dbFlagsLock.unlock();
			}

			try
			{
				rrd.setDataFromQueue(dataSource, data);
			}
			finally
			{
				dbFlagsLock.lock();
				try
				{
					dbFlags.remove(rrdId);
				}
				finally
				{
					dbFlagsLock.unlock();
				}
			}

			calculateAvgWriteTime(System.currentTimeMillis()-startTime);
		}
	}

}
