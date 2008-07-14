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

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.weda.annotations.Description;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass()
public class RRGraphManager extends BaseNode
{
    public final static String STARTINGPOINT_ATTRIBUTE = "startingPoint";
    
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    @Description("The node from which graph manager take a control on dataSources")
    @NotNull
    private Node startingPoint;
    
    private RRGraphManagerTemplate template;

    @Override
    protected void doInit() throws Exception
    {
        super.doInit();
        
        template = (RRGraphManagerTemplate) getChildren(RRGraphManagerTemplate.NAME);
        if (template==null)
        {
            template = new RRGraphManagerTemplate();
            addChildren(template);
            template.save();
            template.init();
        }
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        
        
    }

    public Node getStartingPoint()
    {
        return startingPoint;
    }
}
