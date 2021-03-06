/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.tree.impl;

import org.raven.ds.impl.RecordSchemasNode;

/**
 *
 * @author Mikhail Titov
 */
public class SchemasNode extends BaseNode
{
    public final static String NAME = "Schemas";

    public SchemasNode()
    {
        super(NAME);
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        RecordSchemasNode schemasNode = (RecordSchemasNode) getChildren(RecordSchemasNode.NAME);
        if (schemasNode==null)
        {
            schemasNode = new RecordSchemasNode();
            this.addAndSaveChildren(schemasNode);
            schemasNode.start();
        }
    }
}
