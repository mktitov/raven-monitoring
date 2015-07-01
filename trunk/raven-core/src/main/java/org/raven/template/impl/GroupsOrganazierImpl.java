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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.raven.template.GroupNode;
import org.raven.template.GroupsOrganazier;
import org.raven.template.GroupsOrganizerListener;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeTuner;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class GroupsOrganazierImpl implements GroupsOrganazier
{
	@Service
	private Tree tree;

	public void organize(
			Node target, Node template, NodeTuner tuner, GroupsOrganizerListener listener
			, boolean autoStart)
	{
        Collection<Node> tempChilds = template.getEffectiveChildrens();
        if (tempChilds==null || tempChilds.size()==0)
		{
			target.getLogger().warn("Can't generate nodes from template because of it's empty");
			return;
		}

        Node gTemplate = tempChilds.iterator().next();
        Node injectingPoint = target;
		NodeAttribute groupExpr;
        while ((groupExpr=gTemplate.getNodeAttribute(GroupNode.GROUPINGEXPRESSION_ATTRIBUTE))!=null)
        {
			String groupName = groupExpr.getValue();
			if (groupName==null)
			{
				target.getLogger().error(String.format(
						"Error generating nodes from template. " +
						"The value of the (%s) attribute of the node (%s) is NULL"
						, GroupNode.GROUPINGEXPRESSION_ATTRIBUTE, gTemplate.getPath()));
				return;
			}
			Node nextInjectingPoint = injectingPoint.getChildren(groupName);
			if (nextInjectingPoint==null)
			{
				break;
			}
			else
			{
				injectingPoint = nextInjectingPoint;
				Collection<Node> childs = gTemplate.getEffectiveChildrens();
				if (childs==null || childs.size()==0)
				{
					target.getLogger().error(String.format(
							"Error generating nodes from template. " +
							"The template node (%s) must have a child nodes"
							, gTemplate));
					return;
				}
				gTemplate = childs.iterator().next();
			}
        }

		if (tuner==null)
			tuner = new GroupsOrganizerNodeTuner();

        if (groupExpr!=null)
        {
            //cloning subtree
            Node result = tree.copy(gTemplate, injectingPoint, null, tuner, true, false, true);
			if (autoStart)
				tree.start(result, false);
        }
        else
        {
			if (listener!=null)
				listener.beforeAddOrdinaryNodes(injectingPoint);
			
            //cloning all nodes inside group node (the parent of the gTemplate)
            Collection<Node> childs = gTemplate.getParent().getEffectiveChildrens();

            if (childs!=null)
            {
                List<Node> clonedNodes = new ArrayList<Node>();
                for (Node node: childs)
                    clonedNodes.add(
                        tree.copy(node, injectingPoint, null, tuner, true, false, true));

                for (Node node: clonedNodes)
                {
                    Collection<NodeAttribute> attrs = node.getNodeAttributes();
                    if (attrs!=null)
                        for (NodeAttribute attr: attrs)
                            revalidateAttributeExpression(attr, node, target.getLogger());

					if (autoStart)
						tree.start(node, false);
                }
            }
        }
	}

    private void revalidateAttributeExpression(NodeAttribute attr, Node node, Logger logger)
	{
        if (!attr.isExpressionValid())
		{
            try
			{
                attr.validateExpression();
            }
			catch (Exception ex)
			{
                logger.warn(
						String.format(
							"Error validating expression for attribute (%s)" + " of node (%s)"
							, attr.getName(), node.getPath())
						, ex);
            }
        }
    }
}
