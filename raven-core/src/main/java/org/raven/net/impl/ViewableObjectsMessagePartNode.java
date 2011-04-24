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

package org.raven.net.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.raven.RavenUtils;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.log.LogLevel;
import org.raven.net.MailMessagePart;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.RefreshAttributeValueHandlerFactory;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=MailWriterNode.class)
public class ViewableObjectsMessagePartNode extends BaseNode implements MailMessagePart
{
    public final static String SOURCE_ATTR = "source";

    @NotNull @Parameter(valueHandlerType=NodeReferenceValueHandlerFactory.TYPE)
    private Node source;

    @Parameter
    private String fileName;

    @NotNull @Parameter
    private String contentType;

    @Parameter
    private String refreshAttributes;

    public String getRefreshAttributes() {
        return refreshAttributes;
    }

    public void setRefreshAttributes(String refreshAttributes) {
        this.refreshAttributes = refreshAttributes;
    }

    public Node getSource() {
        return source;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object getContent(DataContext context) throws Exception
    {
        String[] addRefAttrs = RavenUtils.split(refreshAttributes);
        Set addRefAttrsSet = Collections.EMPTY_SET;
        if (addRefAttrs!=null && addRefAttrs.length>0) {
            addRefAttrsSet = new HashSet();
            addRefAttrsSet.addAll(Arrays.asList(addRefAttrs));
        }
        
        Map<String, NodeAttribute> refAttrs = new HashMap<String, NodeAttribute>();
        for (NodeAttribute attr: getNodeAttributes())
            if (   SOURCE_ATTR.equals(attr.getParentAttribute())
                || RefreshAttributeValueHandlerFactory.TYPE.equals(attr.getValueHandlerType())
                || addRefAttrsSet.contains(attr.getName()))
            {
                refAttrs.put(attr.getName(), attr);
            }
        StringBuilder builder = new StringBuilder("<html>")
            .append("<head>")
            .append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=")
            .append(((MailWriterNode)getEffectiveParent()).getContentEncoding())
            .append("\"/>")
            .append("<style>")
            .append("table { border:2px solid; border-collapse: collapse; }")
            .append("th { border:2px solid; }")
            .append("td { border:1px solid; }")
            .append("</style>")
            .append("</head>")
            .append("<body>");
        for (ViewableObject vo: RavenUtils.getSelfAndChildsViewableObjects(source, refAttrs)){
            builder.append("<div>");
            RavenUtils.viewableObjectToHtml(vo, builder);
            builder.append("</div>");
        }
        builder.append("</body></html>");
        return builder.toString();
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();
        syncRefreshAttributes();
    }

    @Override
    public void nodeAttributeValueChanged(Node node, NodeAttribute attribute, Object oldValue, Object newValue)
    {
        super.nodeAttributeValueChanged(node, attribute, oldValue, newValue);
        if (node==this && Status.STARTED.equals(getStatus()) && newValue!=null && SOURCE_ATTR.equals(attribute.getName()))
            try {
                syncRefreshAttributes();
            } catch (Exception ex) {
                if (isLogLevelEnabled(LogLevel.ERROR))
                    getLogger().error(String.format(
                            "Error synchronizing with refresh attributes of the source node (%s)"
                            , source.getPath())
                        , ex);
            }
    }

    private void syncRefreshAttributes() throws Exception
    {
        Map<String, NodeAttribute> refAttrs = RavenUtils.getSelfAndChildsRefreshAttributes(source);
        for (NodeAttribute attr: getNodeAttributes())
            if (SOURCE_ATTR.equals(attr.getParentAttribute()))
                if (!refAttrs.containsKey(attr.getName()))
                    tree.removeNodeAttribute(attr);
        for (NodeAttribute refAttr: refAttrs.values())
            if (getNodeAttribute(refAttr.getName())==null){
                NodeAttribute attr = new NodeAttributeImpl(
                        refAttr.getName(), refAttr.getType(), refAttr.getValue(), null);
                attr.setDescriptionContainer(refAttr.getDescriptionContainer());
                attr.setValueHandlerType(refAttr.getValueHandlerType());
                attr.setParentAttribute(SOURCE_ATTR);
                attr.setOwner(this);
                attr.init();
                addNodeAttribute(attr);
                attr.save();
            }
    }
}
