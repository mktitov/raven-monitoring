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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.ValueValidator;
import org.raven.ds.RecordSchemaField;
import org.raven.ds.RecordSchemaFieldType;
import org.raven.ds.ReferenceValuesSource;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.impl.BaseNode;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;
import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(
    parentNode=RecordSchemaNode.class,
    childNodes={
        CustomReferenceValuesSourceNode.class, ReferenceToReferenceValuesSourceNode.class,
        CustomValueValidatorNode.class, RequiredValueValidatorNode.class,
        MinLengthValidatorNode.class, MaxLengthValidatorNode.class,
        RegexpValidatorNode.class
    })
public class RecordSchemaFieldNode extends BaseNode implements RecordSchemaField
{
    private static final String DEFAULT_VALUE_ATTR = "defaultValue";
    
    @Parameter
    private String displayName;

    @Parameter @NotNull
    private RecordSchemaFieldType fieldType;

    @Parameter
    private String pattern;

    @Message
    private static String conversationErrorMessage;

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public RecordSchemaFieldType getFieldType()
    {
        return fieldType;
    }

    public void setFieldType(RecordSchemaFieldType fieldType)
    {
        this.fieldType = fieldType;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public <E> E getFieldExtension(Class<E> extensionType, String extensionName) {
        for (E ext: NodeUtils.getChildsOfType(this, extensionType))
            if (extensionName==null || extensionName.equals(((Node)ext).getName()))
                return ext;
        return null;
    }

    public ReferenceValuesSource getReferenceValuesSource()
    {
        return getFieldExtension(ReferenceValuesSource.class, null);
    }

    public Collection<String> validate(Object value) {
        Object val;
        Class type = getFieldType().getType();
        try {
            val = converter.convert(type, value, getPattern());
        } catch(TypeConverterException e) {
            return Arrays.asList(String.format(conversationErrorMessage, value, type.getName()));
        }
        List<ValueValidator> validators =
                NodeUtils.getChildsOfType(this, ValueValidator.class);
        if (validators.isEmpty())
            return null;
        ArrayList<String> errors = new ArrayList<String>(validators.size());
        for (ValueValidator validator: validators) {
            String error = validator.validate(val);
            if (error!=null)
                errors.add(error);
        }
        return errors.isEmpty()? null : errors;
    }

    /**
     * Creates new field for the record.
     * @param owner the record schema node
     * @param name the name of the field
     * @param displayName display name for the field
     * @param type the type of the field
     * @param pattern the pattern for field value conversion to/from string
     * @return The reference to the field or null if the field with this name already exists
     */
    public static RecordSchemaFieldNode create(
            Node owner, String name, String displayName
            , RecordSchemaFieldType type, String pattern)
    {
        if (owner.getNode(name)!=null)
            return null;
        RecordSchemaFieldNode field = new RecordSchemaFieldNode();
        field.setName(name);
        owner.addAndSaveChildren(field);
        field.setDisplayName(displayName);
        field.setFieldType(type);
        field.setPattern(pattern);
        field.start();
        return field;
    }

    public Object getFieldDefaultValue() {
        NodeAttribute attr = getAttr(DEFAULT_VALUE_ATTR);
        return attr==null? null : attr.getRealValue();
    }
}
