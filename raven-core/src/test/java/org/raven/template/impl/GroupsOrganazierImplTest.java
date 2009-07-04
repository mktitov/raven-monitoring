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

import org.junit.Test;
import org.raven.test.RavenCoreTestCase;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.template.GroupNode;
import org.raven.template.GroupsOrganazier;
import org.raven.template.impl.objects.NodeWithExpressionVariables;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.ContainerNode;
import org.raven.tree.impl.NodeAttributeImpl;

/**
 *
 * @author Mikhail Titov
 */
public class GroupsOrganazierImplTest extends RavenCoreTestCase
{
	@Test
	public void test() throws Exception
	{
		GroupsOrganazier groupsOrganazier = registry.getService(GroupsOrganazier.class);
		assertNotNull(groupsOrganazier);

		NodeWithExpressionVariables template = new NodeWithExpressionVariables();
		template.setName("template");
		tree.getRootNode().addChildren(template);
		template.save();
		template.init();
		template.start();
		assertEquals(Status.STARTED, template.getStatus());

		ContainerNode nodes = new ContainerNode("nodes");
		tree.getRootNode().addChildren(nodes);
		nodes.save();
		nodes.init();
		nodes.start();
		assertEquals(Status.STARTED, nodes.getStatus());

		GroupNode gr1 = new GroupNode();
		gr1.setName("group");
		template.addChildren(gr1);
		gr1.save();
		gr1.init();
		gr1.setGroupingExpression("'gr1'");

		ContainerNode gr2 = new ContainerNode("another group");
		gr1.addChildren(gr2);
		gr2.save();
		gr2.init();
		NodeAttribute grAttr = new NodeAttributeImpl(
				GroupNode.GROUPINGEXPRESSION_ATTRIBUTE, String.class, null, null);
		grAttr.setValueHandlerType(ExpressionAttributeValueHandlerFactory.TYPE);
		grAttr.setValue("ind");
		gr2.addNodeAttribute(grAttr);
		grAttr.setOwner(gr2);
		grAttr.init();
		grAttr.save();

		ContainerNode node = new ContainerNode("node");
		gr2.addChildren(node);
		node.save();
		node.init();

		template.setVariable("ind", "1");
		groupsOrganazier.organize(nodes, template, null, null, true);

		Node gr1Node = nodes.getChildren("gr1");
		assertNotNull(gr1Node);
		assertTrue(gr1Node instanceof ContainerNode);

		Node gr2Node = gr1Node.getChildren("1");
		assertNotNull(gr2Node);
		assertTrue(gr2Node instanceof ContainerNode);

		Node nodeNode = gr2Node.getChildren("node");
		assertNotNull(nodeNode);
		assertEquals(Status.STARTED, nodeNode.getStatus());

		template.setVariable("ind", "2");
		groupsOrganazier.organize(nodes, template, null, null, false);

		Node gr3Node = gr1Node.getChildren("2");
		assertNotNull(gr3Node);
		assertTrue(gr3Node instanceof ContainerNode);

		nodeNode = gr3Node.getChildren("node");
		assertNotNull(nodeNode);
		assertEquals(Status.INITIALIZED, nodeNode.getStatus());
	}
}