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

import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.weda.converter.TypeConverterException;
import org.weda.services.TypeConverter;

/**
 *
 * @author Mikhail TItov
 */
public class DatabaseFilterElement
{
    public enum ExpressionType
    {
        /**
         * The filter element has an empty expression
         */
        EMPTY,
        /**
         * The filter element has the expression of type:
         *<code>field_name operator value</code>
         */
        OPERATOR,
        /**
         * The filter has expression formed by user
         */
        COMPLETE
    };
    
    public enum OperatorType
    {
        /**
         * The simple operator is one of the: <b>>=, &lt;=, &lt;>, =, >, &lt;</b>
         */
        SIMPLE,
        /**
         * LIKE operator:  <b>field_name</b> <code>LIKE</code> <b>value</b>
         */
        LIKE,
        /**BETWEEN operator:
         * <b>field_name</b> BETWEEN <b>value_1</b> and <b>value_2</b>
         */
        BETWEEN,
        /**IN operator:
         * <b>field_name</b> IN (<b>value_1</b>, <b>value_2</b>, ..., <b>value_n</b>)
         */
        IN
    };

    private final static String[] BETWEEN_OPERATOR = new String[]{"[", "]"};
    private final static String[] IN_OPERATOR = new String[]{"{", "}"};
    private final static char LIST_SEPARTOR = ',';
    private final static char QUOTE_CHAR = '"';

    protected static final String[] operators = {">=", "<=", "<>", "=", ">", "<"};

    private final String columnName;
    private final Class columnType;
    private final String convertPattern;
    private final TypeConverter converter;

    private ExpressionType expressionType = ExpressionType.EMPTY;
    private OperatorType operatorType;
    private String operator;
    private Object value;

    public DatabaseFilterElement(
            String columnName, Class columnType, String convertPattern, TypeConverter converter)
    {
        this.columnName = columnName;
        this.columnType = columnType;
        this.convertPattern = convertPattern;
        this.converter = converter;
    }

    public Object getValue()
    {
        return value;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public ExpressionType getExpressionType()
    {
        return expressionType;
    }

    public String getOperator()
    {
        return operator;
    }
    
    public void setExpression(String expression) throws DatabaseFilterElementException
    {
        expressionType = ExpressionType.OPERATOR;
        String[] values = null;
        if( expression == null || expression.trim().length() == 0 )
        {
            expressionType = ExpressionType.EMPTY;
            value = null;
            operator = null;
        }
        else
        {
            expression = expression.trim();
            //searching for track of the #expression
            if       (expression.charAt(0) == '#')
            {
                if (expression.length()==1)
                {
                    expressionType = ExpressionType.EMPTY;
                    value = null;
                    operator = null;
                }else{
                    expressionType = ExpressionType.COMPLETE;
                    value = expression.substring(1);
                }
            }else {
                boolean isFound = false;
                //searching for operator
                for( int i = 0; i < operators.length; ++i ){
                    if( expression.indexOf(operators[i]) == 0 ){
                        operator = operators[i];
                        value    =
                            expression.substring(operators[i].length()).trim();
                        isFound  = true;
                        operatorType = OperatorType.SIMPLE;
                        break;
                    }
                }
                //if operator not found ==> the searching for LIKE
                if( !isFound ){
                    //searching for BETWEEN operator
                    if (   expression.startsWith(BETWEEN_OPERATOR[0])
                        && expression.endsWith(BETWEEN_OPERATOR[1]))
                    {
                        values = extractValuesFromList(
                                expression.substring(1, expression.length()-1));
                        if (values==null || values.length!=2)
                            throw new DatabaseFilterElementException(String.format(
                                    "Invalid BETWEEN expression - (%s)", expression));
                        operatorType = OperatorType.BETWEEN;
                    }else if (   expression.startsWith(IN_OPERATOR[0])
                              && expression.endsWith(IN_OPERATOR[1]))
                    {
                        values = extractValuesFromList(
                                expression.substring(1, expression.length()-1));
                        if (values==null || values.length==0)
                            throw new DatabaseFilterElementException(String.format(
                                    "Invalid IN expression - (%s)", expression));
                        operatorType = OperatorType.IN;
                    }else if( expression.indexOf("%") != -1 || expression.indexOf("_") != -1)
                    {
                        isFound  = true;
                        value    = expression;
                        //operator = "LIKE";
                        operatorType = OperatorType.LIKE;
                    }else{
                        //if LIKE operator not found then its a equals operator
                        operator = "=";
                        value    = expression;
                        operatorType = operatorType.SIMPLE;
                    }
                }
                if (values == null)
                {
                    if (operatorType != OperatorType.LIKE)
                        value = convertStringToValueType(value);
                }else {
                    Object[] tempValues = new Object[values.length];
                    for (int i=0; i<values.length; ++i)
                        tempValues[i] = convertStringToValueType(values[i]);
                    value = tempValues;
                }
            }
        }
    }

    private static String[] extractValuesFromList(String listString)
    {
        StrTokenizer tokenizer = new StrTokenizer(listString, LIST_SEPARTOR, QUOTE_CHAR);
        tokenizer.setTrimmerMatcher(StrMatcher.trimMatcher());
        return tokenizer.getTokenArray();
    }

    private Object convertStringToValueType(Object value) throws DatabaseFilterElementException
    {
        try
        {
            return converter.convert(columnType, value, convertPattern);
        }
        catch(TypeConverterException e)
        {
            throw new DatabaseFilterElementException(e.getMessage(), e);
        }
    }

    public OperatorType getOperatorType()
    {
        return operatorType;
    }
}
