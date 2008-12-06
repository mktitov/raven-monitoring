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

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.expr.Expression;
import org.raven.expr.impl.ExpressionAttributeValueHandler;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.statdb.StatisticsRecord;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=StatisticsDefinitionsNode.class)
public class StatisticsDefinitionNode extends BaseNode
{
	@Parameter
	@NotNull
	private String type;

	@Parameter(defaultValue="false")
	@NotNull
	private Boolean useValueExpression;

	@Parameter(valueHandlerType=ExpressionAttributeValueHandlerFactory.TYPE)
	private Double valueExpression;

	@Parameter(defaultValue="false")
	@NotNull
	private Boolean savePreviousValue;

	private Double previuosValue;

	public Boolean getSavePreviousValue()
	{
		return savePreviousValue;
	}

	public void setSavePreviousValue(Boolean savePreviousValue)
	{
		this.savePreviousValue = savePreviousValue;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public Boolean getUseValueExpression()
	{
		return useValueExpression;
	}

	public void setUseValueExpression(Boolean useValueExpression)
	{
		this.useValueExpression = useValueExpression;
	}

	public Double getValueExpression()
	{
		return valueExpression;
	}

	public void setValueExpression(Double valueExpression)
	{
		this.valueExpression = valueExpression;
	}

	public Double calculateValue(Double value, StatisticsRecord record) throws ScriptException
	{
		Double newValue = value;
		NodeAttribute expAttr = getNodeAttribute("valueExpression");
		if (   useValueExpression
			&& expAttr.getValueHandler() instanceof ExpressionAttributeValueHandler)
		{
			Expression expression = 
					(Expression)(ExpressionAttributeValueHandler) expAttr.getValueHandler();
			Bindings bindings = new SimpleBindings();
			bindings.put("node", this);
			bindings.put("lastValue", value);
			bindings.put("record", value);
			if (savePreviousValue)
				bindings.put("previousValue", previuosValue);
			newValue = (Double) expression.eval(bindings);
		}

		return newValue;
	}

}
