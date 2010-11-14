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

package org.raven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.raven.ds.RecordSchema;
import org.raven.ds.RecordSchemaField;
import org.raven.table.ColumnGroup;
import org.raven.table.ColumnGroupImpl;
import org.raven.table.ColumnGroupTag;
import org.raven.table.Table;
import org.raven.table.TableTag;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.impl.BaseNode;
import org.weda.internal.annotations.Service;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail Titov
 */
public class RavenUtils
{
    public static final String DEFAULT_SPLIT_DELIMITER = ",";

    @Service
    public static TypeConverter converter;
    
    private RavenUtils(){ }

    /**
     * Splits the string using {@link #DEFAULT_SPLIT_DELIMITER DEFAULT_SPLIT_DELIMITER}.
     * @return returns null if str is null or array of strings
     */
    public static String[] split(String str)
    {
        return split(str, DEFAULT_SPLIT_DELIMITER);
    }

    public static String nameToDbName(String name)
    {
        if (name==null)
            return null;
        
        boolean isUpperCase = true;
        for (int i=0; i<name.length(); ++i)
            if (Character.isLetter(name.charAt(i)) && Character.isLowerCase(name.charAt(i)))
            {
                isUpperCase = false;
                break;
            }
        if (isUpperCase)
            return name;
        
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<name.length(); ++i) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch))
                buf.append("_");
            buf.append((""+ch).toUpperCase());
        }
        
        return buf.toString();
    }
    
    /**
     * Splits the string.
     * @return returns null if str is null or array of strings
     */
    public static String[] split(String str, String delimiter)
    {
        if (str==null)
            return null;
        StrTokenizer tokenizer = new StrTokenizer(str, delimiter);
        tokenizer.setTrimmerMatcher(StrMatcher.trimMatcher());
        tokenizer.setQuoteChar('"');

        return tokenizer.getTokenArray();
    }

    public static List<Object[]> tableAsList(Table table)
    {
        if (table==null)
            return null;
        else
        {
            List<Object[]> result = new ArrayList<Object[]>();
            for (Iterator<Object[]> it=table.getRowIterator(); it.hasNext();)
                result.add(it.next());

            return result;
        }
    }

//    public static Table hideTableColumns(Table table, int[] columns)
//    {
//        if (table==null)
//            return null;
//        TableImpl
//    }

    /**
     * Returns the record schema field or null if schema does not contains the field with name
     * passed in parameter
     * @param recordSchema record schema
     * @param fieldName field name
     */
    public static RecordSchemaField getRecordSchemaField(
            RecordSchema recordSchema, String fieldName)
    {
        RecordSchemaField[] fields = recordSchema.getFields();
        if (fields!=null)
            for (RecordSchemaField field: fields)
                if (fieldName.equals(field.getName()))
                    return field;

        return null;
    }

    public static Map<String, RecordSchemaField> getRecordSchemaFields(RecordSchema recordSchema)
    {
        RecordSchemaField[] fields = recordSchema.getFields();
        if (fields!=null)
        {
            Map<String, RecordSchemaField> fieldsMap = new HashMap<String, RecordSchemaField>();
            for (RecordSchemaField field: fields)
                fieldsMap.put(field.getName(), field);
            return fieldsMap;
        }
        else
            return null;
    }

	public static void copyAttributes(
			Node sourceNode, Node targetNode, boolean initAndSaveAttribute
            , String... excludeAttributes)
		throws Exception
	{
		Collection<NodeAttribute> attrs = sourceNode.getNodeAttributes();
		if (attrs!=null)
		{
			for (NodeAttribute attr: attrs)
			{
				if (   (   excludeAttributes==null
					    || !ArrayUtils.contains(excludeAttributes, attr.getName()))
					&& targetNode.getNodeAttribute(attr.getName())==null
					&& !attr.getName().equals(BaseNode.LOGLEVEL_ATTRIBUTE))
				{
					NodeAttribute attrClone = (NodeAttribute) attr.clone();
					attrClone.setOwner(targetNode);
					targetNode.addNodeAttribute(attrClone);
					if (initAndSaveAttribute)
					{
						attrClone.init();
						attrClone.save();
					}
				}
			}
		}
	}

    public static List<ColumnGroup> getTableColumnGroups(Table table)
    {
        List<ColumnGroup> groups = new LinkedList<ColumnGroup>();
        ColumnGroupImpl group = null;
        String[] colNames = table.getColumnNames();
        for (int i=0; i<colNames.length; ++i)
        {
            if (group!=null && i<=group.getToColumn())
                group.addColumnName(colNames[i]);
            else {
                group = null;
                Map<String, TableTag> tags = table.getColumnTags(i);
                if (tags!=null && !tags.isEmpty()){
                    for (TableTag tag: tags.values())
                        if (tag instanceof ColumnGroupTag){
                            ColumnGroupTag groupTag = (ColumnGroupTag) tag;
                            group = new ColumnGroupImpl(
                                    groupTag.getId(), groupTag.getFromColumn(), groupTag.getToColumn());
                            group.addColumnName(colNames[i]);
                            break;
                        }
                }
                if (group==null)
                    group = new ColumnGroupImpl(colNames[i], i, i);
                groups.add(group);
            }
        }

        return groups;
    }

    public static StringBuilder tableToHtml(Table table, StringBuilder builder)
    {
        if (builder==null)
            builder = new StringBuilder("<table>");
        else
            builder.append("<table>");

        boolean hasGroups = false;
        List<ColumnGroup> groups = RavenUtils.getTableColumnGroups(table);
        for (ColumnGroup group: groups)
            if (group.isHasNestedColumns()) {
                hasGroups = true;
                break;
            }

        builder.append("<tr>");
        StringBuilder h = null;
        if (hasGroups)
            h = new StringBuilder("<tr>");
        for (ColumnGroup group: groups){
            String name = group.getGroupName();
            if (!group.isHasNestedColumns()){
                builder.append("<th");
                if (hasGroups) builder.append(" rowspan=\"2\"");
                builder.append(">").append(name==null||name.isEmpty()? "&nbsp;" : name).append("</th>");
            }else{
                builder.append("<th colspan=\"").append(group.getColumnNames().size()).append("\">")
                        .append(name==null||name.isEmpty()? "&nbsp;" : name)
                        .append("</th>");
                for (String columnName: group.getColumnNames())
                    h.append("<th>").append(columnName==null||columnName.isEmpty()? "&nbsp;" : columnName)
                        .append("</th>");
            }
        }
        builder.append("</tr>");
        if (hasGroups){
            builder.append(h);
            builder.append("</tr>");
        }

        Iterator<Object[]> it = table.getRowIterator();
        while (it.hasNext()){
            builder.append("<tr>");
            for (Object value: it.next()){
                String pattern = null;
                if (value instanceof java.sql.Date)
                    pattern = "dd.MM.yyyy";
                else if (value instanceof java.sql.Time)
                    pattern = "HH:mm:ss";
                else if(value instanceof java.util.Date)
                    pattern = "dd.MM.yyyy HH:mm:ss";
                else if(value instanceof Double || value instanceof Float)
                    pattern = "0.00";
                String strValue = converter.convert(String.class, value, pattern);
                if (strValue==null || strValue.isEmpty())
                    strValue = "&nbsp;";
                builder.append("<td>").append(strValue).append("</td>");
            }
            builder.append("</tr>");
        }

        builder.append("</table>");

        return builder;
    }

    public static StringBuilder viewableObjectToHtml(ViewableObject obj, StringBuilder builder)
    {
        if (builder==null)
            builder = new StringBuilder();
        if (Viewable.RAVEN_TABLE_MIMETYPE.equals(obj.getMimeType()))
            tableToHtml((Table)obj.getData(), builder);
        else if (Viewable.RAVEN_TEXT_MIMETYPE.equals(obj.getMimeType()))
            builder.append(obj.getData());
        return builder;
    }

    /**
     * Returns the map of the refresh attributes of the node and it's child nodes or empty map if
     * node and it's child nodes does not contain refresh attributes
     * @param node the source node
     * @throws Exception
     */
    public static Map<String, NodeAttribute> getSelfAndChildsRefreshAttributes(Node node) throws Exception
    {
        Map<String, NodeAttribute> attrs = new HashMap<String, NodeAttribute>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        nodes.add(node);
        List<Node> childs = node.getSortedChildrens();
        if (childs!=null)
            nodes.addAll(childs);
        for (Node n: nodes)
            if (n instanceof Viewable){
                Map<String, NodeAttribute> refAttrs = ((Viewable)n).getRefreshAttributes();
                if (refAttrs!=null)
                    attrs.putAll(refAttrs);
            }
        return attrs.isEmpty()? Collections.EMPTY_MAP : attrs;
    }

    public static Collection<ViewableObject> getSelfAndChildsViewableObjects(
            Node node, Map<String, NodeAttribute> refreshAttributes)
        throws Exception
    {
        ArrayList<ViewableObject> vos = new ArrayList<ViewableObject>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        nodes.add(node);
        List<Node> childs = node.getSortedChildrens();
        if (childs!=null)
            nodes.addAll(childs);
        for (Node n: nodes)
            if (n instanceof Viewable){
                Collection<ViewableObject> objects = ((Viewable)n).getViewableObjects(refreshAttributes);
                if (objects!=null)
                    vos.addAll(objects);
            }
        return vos.isEmpty()? Collections.EMPTY_LIST : vos;
    }
}
