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

package org.raven.expr.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;

/**
 *
 * @author Mikhail Titov
 */
public class ExpressionCacheImpl implements ExpressionCache
{
	private final Map<String, Expression> cache = new HashMap<String, Expression>(1024);
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public Expression getExpression(String key)
	{
		try
		{
			lock.readLock().lock();
			return cache.get(key);
		}
		finally
		{
			lock.readLock().unlock();
		}
	}

	public void putExpression(String key, Expression expression)
	{
		try
		{
			lock.writeLock().lock();
			cache.put(key, expression);
		}
		finally
		{
			lock.writeLock().unlock();
		}
	}
}
