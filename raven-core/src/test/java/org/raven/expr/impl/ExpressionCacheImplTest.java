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

import org.junit.Assert;
import org.junit.Test;
import org.raven.expr.Expression;
import org.raven.expr.ExpressionCache;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class ExpressionCacheImplTest extends Assert
{
	@Test
	public void test()
	{
		Expression expression = createMock(Expression.class);
		ExpressionCache cache = new ExpressionCacheImpl();
		cache.putExpression("1+1", expression);
		assertSame(expression, cache.getExpression("1+1"));
	}
}