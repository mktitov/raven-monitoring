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

package org.raven.template.impl;

import org.raven.template.GroupNode;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.impl.ContainerNode;

/**
 *
 * @author Mikhail Titov
 */
public class GroupsOrganizerNodeTuner extends TemplateExpressionNodeTuner
{
	@Override
	public Node cloneNode(Node sourceNode)
	{
		if (sourceNode instanceof GroupNode)
		{
			String groupName = getGroupName(sourceNode);
			ContainerNode clone = new ContainerNode(groupName);

			return clone;
		}
		else
			return null;
	}

	@Override
	public void tuneNode(Node sourceNode, Node sourceClone)
	{
		super.tuneNode(sourceNode, sourceClone);

		if (sourceNode.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE)!=null)
		{
			String groupName = getGroupName(sourceNode);
			sourceClone.setName(groupName);
			sourceClone.removeNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
		}
	}

	private String getGroupName(Node sourceNode)
	{
		NodeAttribute groupingExpression = sourceNode.getNodeAttribute(
				GroupNode.GROUPINGEXPRESSION_ATTRIBUTE);
		if (groupingExpression==null)
			throw new NodeError(String.format(
					"Error generating nodes from template. " +
					"Node (%s) must contains (%s) attribute"
					, sourceNode.getPath() , GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));

		String groupName = groupingExpression.getValue();
		if (groupName==null)
			throw new NodeError(String.format(
					"Error generating nodes from template. Error in the node (%s). " +
					"The value of the attribute (%s) is NULL"
					, sourceNode.getPath(), GroupNode.GROUPINGEXPRESSION_ATTRIBUTE));

		return groupName;
	}

}
