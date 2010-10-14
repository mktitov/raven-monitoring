/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.rep;

import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=JxlsReportNode.class)
public class CellStyleSelectorNode
{
    @NotNull @Parameter
    private String styleCellLabel;

    @NotNull @Parameter(defaultValue="false")
    private Boolean selector;

    public Boolean getSelector() {
        return selector;
    }

    public void setSelector(Boolean selector) {
        this.selector = selector;
    }

    public String getStyleCellLabel() {
        return styleCellLabel;
    }

    public void setStyleCellLabel(String styleCellLabel) {
        this.styleCellLabel = styleCellLabel;
    }
}
