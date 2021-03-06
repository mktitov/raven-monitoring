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

package org.raven.table;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.impl.AbstractRecordFieldExtension;
import org.raven.ds.impl.FieldCodecReferenceNode;
import org.raven.ds.impl.RecordSchemaFieldCodecNode;
import org.raven.ds.impl.RecordSchemaFieldNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaFieldNode.class, childNodes={
        RecordSchemaFieldCodecNode.class, FieldCodecReferenceNode.class
    })
public class TableColumnRecordFieldExtension extends AbstractRecordFieldExtension
{
    @Parameter @NotNull
    private Integer columnNumber;

    public Integer getColumnNumber()
    {
        return columnNumber;
    }

    public void setColumnNumber(Integer columnNumber)
    {
        this.columnNumber = columnNumber;
    }
}
