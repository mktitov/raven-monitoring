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

package org.raven.tree.store.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.raven.tree.DataFile;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.DataFileValueHandlerFactory;

/**
 *
 * @author Mikhail Titov
 */
public class XMLWriter implements XMLConsts
{
    public void write(OutputStream out, String charsetName, Node... nodes)
            throws Exception
    {
        OutputStreamWriter writer = new OutputStreamWriter(out, charsetName);
        try
        {
            writer.append(String.format("<?xml version=\"1.0\" encoding=\"%s\"?>\n", charsetName));
            writer.append("<nodes>\n");
            int offset=2;
            for (Node node: nodes)
                writeNode(offset, node, writer, out);
            writer.append("</nodes>\n");
        }
        finally
        {
            writer.close();
        }
    }

    private void writeNode(int offset, Node node, Writer writer, OutputStream out) throws Exception
    {
        String strOffset = StringUtils.repeat(" ", offset);
        writer.append(strOffset+String.format(
                "<node %s=\"%s\" %s=\"%s\">\n", NAME_ATTRIBUTE, node.getName()
                , CLASS_ATTRIBUTE, node.getClass().getName()));
        offset+=2;
        String childOffset = StringUtils.repeat(" ", offset);
        for (NodeAttribute attr: node.getAttrs()) {
            if (attr.isReadonly())
                continue;
            writer.append(childOffset+String.format(
                    "<attribute %s=\"%s\"", NAME_ATTRIBUTE, attr.getName()));
            if (attr.getParameterName()==null)
            {
                writer.append(String.format(
                        " %s=\"%s\"", TYPE_ATTRIBUTE, attr.getType().getName()));
                if (attr.isRequired())
                    writer.append(String.format(" %s=\"true\"", REQUIRED_ATTRIBUTE));
            }
            if (attr.getValueHandlerType()!=null)
                writer.append(String.format(
                        " %s=\"%s\"", VALUE_HANDLER_ATTRIBUTE, attr.getValueHandlerType()));
            if (attr.isTemplateExpression())
                writer.append(String.format(" %s=\"true\"", TEMPLATE_EXPRESSION_ATTRIBUTE));
            if (attr.getParentAttribute()!=null)
                writer.append(String.format(
                        " %s=\"%s\"", PARENT_ATTRIBUTE, attr.getParentAttribute()));
            InputStream data = 
                    DataFile.class.isAssignableFrom(attr.getType())
                    && DataFileValueHandlerFactory.TYPE.equals(attr.getValueHandlerType())?
                        ((DataFile)attr.getRealValue()).getDataStream() : null;
            if (   (attr.getRawValue()==null && data==null && attr.getParameterName()==null)
                && (attr.getDescription()==null || attr.getParameterName()!=null))
            {
                writer.append("/>\n");
            }
            else
            {
                writer.append(">\n");
                if (attr.getParameterName()==null && attr.getDescription()!=null)
                {
                    writer.append(childOffset+"  <description><![CDATA[");
                    writer.append(
                            StoreUtils.messageComposerToString(attr.getDescriptionContainer()));
                    writer.append("]]></description>\n");
                }
                if (attr.getRawValue()!=null || data!=null || attr.getParameterName()!=null)
                {
//                    String value = attr.getParameterName()!=null?
                    if (data!=null)
                    {
                        writer.append(childOffset+"  <value><![CDATA[\n");
                        writer.flush();
                        ByteArrayOutputStream dataStream = new  ByteArrayOutputStream();
                        Base64OutputStream coder = new Base64OutputStream(dataStream, true);
                        IOUtils.copy(data, coder);
                        coder.close();
                        out.write(dataStream.toByteArray());
                        writer.append("\n"+childOffset+"  ]]></value>\n");
                    }
                    else
                    {
                        if (attr.getRawValue()==null)
                            writer.append(childOffset+"  <value></value>\n");
                        else
                        {
                            writer.append(childOffset+"  <value><![CDATA[");
                            writer.append(attr.getRawValue());
                            writer.append("]]></value>\n");
                        }
                    }
                }
                writer.append(childOffset+"</attribute>\n");
            }
        }
        Collection<Node> childs = node.getSortedChildrens();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                writeNode(offset, child, writer, out);
        writer.append(strOffset+"</node>\n");
    }
}
