/*
 *  Copyright 2009 Mikhail Titov .
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

import javax.script.Bindings;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=RecordSchemaFieldNode.class, childNodes=ValuePrepareRecordFieldExtension.class)
public class CsvRecordFieldExtension extends AbstractRecordFieldExtension
{
    @Parameter @NotNull
    private Integer columnNumber;


    public Integer getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(Integer columnNumber) {
        this.columnNumber = columnNumber;
    }
    
    public Integer getPreparedColumnNumber(Bindings bindings) {
        return prepareValue(columnNumber, bindings);
    }

    public final static CsvRecordFieldExtension create(Node owner, String extensionName, int columnNumber) {
        if (owner.getNode(extensionName)!=null)
            return null;
        CsvRecordFieldExtension csvExt = new CsvRecordFieldExtension();
        csvExt.setName(extensionName);
        owner.addAndSaveChildren(csvExt);
        csvExt.setColumnNumber(columnNumber);
        csvExt.start();
        return csvExt;
    }
}
