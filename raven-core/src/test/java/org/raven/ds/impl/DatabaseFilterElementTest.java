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

import org.junit.Assert;
import org.junit.Test;
import org.weda.services.TypeConverter;
import static org.easymock.EasyMock.*;
/**
 *
 * @author Mikhail Titov
 */
public class DatabaseFilterElementTest extends Assert
{
    @Test
    public void emptyExpressionTypeTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        replay(converter);
        
        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, converter);

        for (String expression: new String[]{null, "", " "})
        {
            element.setExpression(expression);
            assertEquals(DatabaseFilterElement.ExpressionType.EMPTY, element.getExpressionType());
        }

        verify(converter);
    }

    @Test
    public void getColumnNameWithTableAliasTest() throws Exception
    {
        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, null);
        assertEquals("col", element.getColumnNameWithTableAlias());
    }

    @Test
    public void getColumnNameWithTableAliasTest2() throws Exception
    {
        DatabaseFilterElement element =
                new DatabaseFilterElement("col", "t", Integer.class, null, false, null);
        assertEquals("t.col", element.getColumnNameWithTableAlias());
    }

    @Test
    public void completeExpressionTypeTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        replay(converter);
        
        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, converter);
        
        element.setExpression("#test expression");
        assertEquals(DatabaseFilterElement.ExpressionType.COMPLETE, element.getExpressionType());
        assertEquals("test expression", element.getValue());

        verify(converter);
    }

    @Test
    public void simpleOperatorTypeTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        expect(converter.convert(Integer.class, "1", null)).andReturn(1).atLeastOnce();
        replay(converter);

        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, converter);

        for (String operator: new String[]{">=", "<=", "<>", "=", ">", "<"})
        {
            element.setExpression(operator+"1");
            assertEquals(
                    DatabaseFilterElement.ExpressionType.OPERATOR, element.getExpressionType());
            assertEquals(DatabaseFilterElement.OperatorType.SIMPLE, element.getOperatorType());
            assertEquals(operator, element.getOperator());
            assertEquals(1, element.getValue());
        }

        verify(converter);
    }

    @Test
    public void defaultOperatorTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        expect(converter.convert(Integer.class, "1", "pattern")).andReturn(1);
        replay(converter);

        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, "pattern", false, converter);

        element.setExpression("1");
        assertEquals(DatabaseFilterElement.ExpressionType.OPERATOR, element.getExpressionType());
        assertEquals(DatabaseFilterElement.OperatorType.SIMPLE, element.getOperatorType());
        assertEquals("=", element.getOperator());
        assertEquals(1, element.getValue());

        verify(converter);
    }

    @Test
    public void likeOperatorTypeTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        replay(converter);

        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, converter);

        for (String expression: new String[]{"%1", "1%", "1_", "_1", "1%2_3"})
        {
            element.setExpression(expression);
            assertEquals(
                    DatabaseFilterElement.ExpressionType.OPERATOR, element.getExpressionType());
            assertEquals(DatabaseFilterElement.OperatorType.LIKE, element.getOperatorType());
            assertEquals(expression, element.getValue());
        }
        
        verify(converter);
    }

    @Test
    public void betweenOperatorTypeTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        expect(converter.convert(Integer.class, "1", null)).andReturn(1);
        expect(converter.convert(Integer.class, "2", null)).andReturn(2);
        replay(converter);

        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, converter);

        element.setExpression("[1 , \"2\"]");
        assertEquals(DatabaseFilterElement.ExpressionType.OPERATOR, element.getExpressionType());
        assertEquals(DatabaseFilterElement.OperatorType.BETWEEN, element.getOperatorType());
        assertArrayEquals(new Integer[]{1, 2}, (Object[])element.getValue());

        verify(converter);
    }

    @Test
    public void inOperatorTypeTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        expect(converter.convert(Integer.class, "1", null)).andReturn(1);
        expect(converter.convert(Integer.class, "2", null)).andReturn(2);
        replay(converter);

        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, null, false, converter);

        element.setExpression("{1 , \"2\"}");
        assertEquals(DatabaseFilterElement.ExpressionType.OPERATOR, element.getExpressionType());
        assertEquals(DatabaseFilterElement.OperatorType.IN, element.getOperatorType());
        assertArrayEquals(new Integer[]{1, 2}, (Object[])element.getValue());

        verify(converter);
    }

    @Test
    public void convertPatternTest() throws DatabaseFilterElementException
    {
        TypeConverter converter = createMock(TypeConverter.class);
        expect(converter.convert(Integer.class, "1", "pattern")).andReturn(1);
        replay(converter);

        DatabaseFilterElement element =
                new DatabaseFilterElement("col", null, Integer.class, "pattern", false, converter);

        element.setExpression("=1");

        verify(converter);
    }
}