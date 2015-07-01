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

import org.raven.annotations.NodeClass;
import org.raven.expr.impl.IfNode;
import org.raven.tree.Node;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.InvisibleNode;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=InvisibleNode.class, childNodes={IfNode.class})
public class RulesNode extends BaseNode
{
    public final static String NAME = "Rules";

    public RulesNode()
    {
        super(NAME);
		setSubtreeListener(true);
    }

	@Override
	public void nodeStatusChanged(Node node, Status oldStatus, Status newStatus)
	{
		super.nodeStatusChanged(node, oldStatus, newStatus);
		if (newStatus==Status.INITIALIZED && oldStatus==Status.CREATED && node instanceof IfNode)
		{
			((IfNode)node).setUsedInTemplate(false);
		}
	}

	
}
