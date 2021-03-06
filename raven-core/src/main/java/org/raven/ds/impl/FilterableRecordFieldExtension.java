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
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=RecordSchemaFieldNode.class)
public class FilterableRecordFieldExtension extends BaseNode
{
    public final static String FILTER_VALUE_REQUIRED_ATTR = "filterValueRequired";

    @Parameter(defaultValue="false")
    @NotNull
    private Boolean filterValueRequired;

    @Parameter()
    private String defaultValue;

    @Parameter(defaultValue="true")
    private Boolean caseSensitive;

    public Boolean getCaseSensitive()
    {
        return caseSensitive;
    }

    public void setCaseSensitive(Boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public Boolean getFilterValueRequired()
    {
        return filterValueRequired;
    }

    public void setFilterValueRequired(Boolean filterValueRequired)
    {
        this.filterValueRequired = filterValueRequired;
    }

    public final static FilterableRecordFieldExtension create(
            Node owner, String extensionName, boolean caseSensitive, boolean filterValueRequired,
            String defaultValue)
    {
        if (owner.getChildren(extensionName)!=null)
            return null;
        FilterableRecordFieldExtension filterExt = new FilterableRecordFieldExtension();
        filterExt.setName(extensionName);
        owner.addAndSaveChildren(filterExt);
        filterExt.setCaseSensitive(caseSensitive);
        filterExt.setFilterValueRequired(filterValueRequired);
        filterExt.setDefaultValue(defaultValue);
        filterExt.start();

        return filterExt;
    }
}
