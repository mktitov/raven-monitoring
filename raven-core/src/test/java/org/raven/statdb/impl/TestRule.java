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

package org.raven.statdb.impl;

import org.raven.statdb.Rule;
import org.raven.statdb.RuleProcessingResult;
import org.raven.statdb.StatisticsDatabase;
import org.raven.statdb.StatisticsRecord;
import org.raven.tree.impl.BaseNode;

/**
 *
 * @author Mikhail Titov
 */
public class TestRule extends BaseNode implements Rule
{
	private Rule ruleMock;

	public Rule getRuleMock()
	{
		return ruleMock;
	}

	public void setRuleMock(Rule ruleMock)
	{
		this.ruleMock = ruleMock;
	}

	public void processRule(
			String key, String name, Double value, StatisticsRecord record
			, RuleProcessingResult result, StatisticsDatabase database)
	{
		ruleMock.processRule(key, name, value, record, result, database);
	}

}
