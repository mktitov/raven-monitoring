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

package org.raven.tree.impl;

import java.util.*;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 * The node that references to other node
 * @author Mikhail Titov
 */
@NodeClass
public class ReferenceNode extends BaseNode implements Viewable
{
    @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private Node reference;
    
    @Parameter
    private String hideRefreshAttributes;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useInTemplate;

    @Override
    public boolean isConditionalNode() {
        return (isTemplate() && useInTemplate) || (!isTemplate() && !useInTemplate);
    }

    @Override
    public Collection<Node> getEffectiveChildrens() {
        if (!isConditionalNode())
            return null;
        Node refNode = reference;
        if (refNode==null)
            return null;
        else if (refNode.isConditionalNode())
            return refNode.getEffectiveChildrens();
        else
            return Arrays.asList(refNode);
    }

    public String getHideRefreshAttributes() {
        return hideRefreshAttributes;
    }

    public void setHideRefreshAttributes(String hideRefreshAttributes) {
        this.hideRefreshAttributes = hideRefreshAttributes;
    }

    public Node getReference() {
        return reference;
    }

    public void setReference(Node reference) {
        this.reference = reference;
    }

    public Boolean getUseInTemplate() {
        return useInTemplate;
    }

    public void setUseInTemplate(Boolean useInTemplate) {
        this.useInTemplate = useInTemplate;
    }

    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        Node _reference = reference;
        Map<String, NodeAttribute> refAttrs = null; 
        Map<String, NodeAttribute> selfRefAttrs = NodeUtils.extractRefereshAttributes(this);
        Map<String, NodeAttribute> refRefAttrs = null;
        if (isConditionalNode() && _reference instanceof Viewable) {
            refRefAttrs =  ((Viewable)_reference).getRefreshAttributes();
            if (refRefAttrs!=null && !refRefAttrs.isEmpty()) {
                String[] hideAttrs = RavenUtils.split(hideRefreshAttributes);
                if (hideAttrs!=null)
                    for (String hideAttr: hideAttrs) 
                        refRefAttrs.remove(hideAttr);
            }
        }
        if ( (selfRefAttrs!=null && !selfRefAttrs.isEmpty()) || (refRefAttrs!=null && !refRefAttrs.isEmpty()) )
            refAttrs = new HashMap<String, NodeAttribute>();
        if (refRefAttrs!=null && !refRefAttrs.isEmpty())
            refAttrs.putAll(refRefAttrs);
        if (selfRefAttrs!=null && !selfRefAttrs.isEmpty())
            refAttrs.putAll(selfRefAttrs);
        
        return refAttrs;
    }

    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes)
            throws Exception
    {
        Node _reference = reference;
        refreshAttributes = NodeUtils.concatAttributesMap(refreshAttributes
                , NodeUtils.extractHiddenRefereshAttributes(this));
        if (isConditionalNode() && _reference instanceof Viewable)
            return ((Viewable)_reference).getViewableObjects(refreshAttributes);
        else
            return null;
    }

    public Boolean getAutoRefresh()
    {
        Node _reference = reference;
        if (isConditionalNode() && _reference instanceof Viewable)
            return ((Viewable)_reference).getAutoRefresh();
        else
            return false;
    }

}
