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

package org.raven.ds.impl;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaFieldNode.class)
public class RecordRelationFieldExtension extends BaseNode
{
    @NotNull @Parameter(valueHandlerType=RecordSchemaValueTypeHandlerFactory.TYPE)
    private RecordSchemaNode relatedSchema;
    
    @NotNull @Parameter()
    private String relatedField;
    
    @Parameter
    private String relatedDisplayField;

    public String getRelatedField()
    {
        return relatedField;
    }

    public void setRelatedField(String relatedField)
    {
        this.relatedField = relatedField;
    }

    public RecordSchemaNode getRelatedSchema()
    {
        return relatedSchema;
    }

    public void setRelatedSchema(RecordSchemaNode relatedSchema)
    {
        this.relatedSchema = relatedSchema;
    }

    public String getRelatedDisplayField() {
        return relatedDisplayField;
    }

    public void setRelatedDisplayField(String relatedDisplayField) {
        this.relatedDisplayField = relatedDisplayField;
    }
}
