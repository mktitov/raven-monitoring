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
    private final static String[] LIST_SEPARATORS = new String[]{"<,>", ","};

    protected static final String [] operators = {">=", "<=", "<>", "=", ">", "<"};
    private String id;
    private ExpressionType expressionType = ExpressionType.EMPTY;
    private OperatorType operatorType;
    private String property;
    private boolean enabled = true;
    private boolean useAliasAsProperty = false;
    private Class propertyClass;
    private String pattern;
    private String objectAlias;
    private String parameterName;
    private String staticExpression;
    private String expression;
    private String operator;
    private Object value;
    private PropertyDescriptor propertyDescriptor;
    //
    @InjectHivemindObject private static ValueTypeConverter converter;
    @InjectMessages private Messages messages;

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getStaticExpression() {
        return staticExpression;
    }

    public void setStaticExpression(String staticExpression) {
        this.staticExpression = staticExpression;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String name) {
        property = name;
    }

    public void setPattern(String pattern) {
        this.pattern=pattern;
    }

    public String getObjectAlias() {
        return objectAlias;
    }

    public void setObjectAlias(String objectAlias) {
        this.objectAlias=objectAlias;
    }

    public Object getParameterValue() {
        return value;
    }

    public ExpressionType getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(ExpressionType type) {
        expressionType = type;
    }

    public String getOperator(){
        return operator;
    }

    public void setOperator(String operator){
        this.operator = operator;
    }

    public void setExpression(String expression)
        throws QueryFilterElementException
    {
        expressionType = ExpressionType.OPERATOR;
        String[] values = null;
        if( expression == null || expression.trim().length() == 0 ){
            expressionType = ExpressionType.EMPTY;
            value = null;
            operator = null;
        }else{
            expression = expression.trim();
            //поищем следы #выражения
            if       (expression.charAt(0) == '#'){
                if (expression.length()==1){
                    expressionType = ExpressionType.EMPTY;
                    value = null;
                    operator = null;
                }else{
                    expressionType = ExpressionType.COMPLETE;
                    value = expression.substring(1);
                }
            }else {
                boolean isFound = false;
                //попробуем найти оператор
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
                //Если оператор не найден ==> тогда поищем следы оператора LIKE
                if( !isFound ){
                    //поищем оператор BETWEEN
                    if (   expression.startsWith(BETWEEN_OPERATOR[0])
                        && expression.endsWith(BETWEEN_OPERATOR[1]))
                    {
                        values = extractValuesFromList(
                                expression.substring(1, expression.length()-1));
                        if (values==null || values.length!=2)
                            throw new QueryFilterElementException(
                                    messages.format(
                                        "BetweenOperatorError", expression));
                        operatorType = OperatorType.BETWEEN;
                    }else if (   expression.startsWith(IN_OPERATOR[0])
                              && expression.endsWith(IN_OPERATOR[1]))
                    {
                        values = extractValuesFromList(
                                expression.substring(1, expression.length()-1));
                        if (values==null || values.length==0)
                            throw new QueryFilterElementException(
                                    messages.format(
                                        "InOperatorError", expression));
                        operatorType = OperatorType.IN;
                    }else if(    propertyClass.equals(java.lang.String.class)
                             && (   expression.indexOf("%") != -1
                                 || expression.indexOf("_") != -1 ) )
                    {
                        isFound  = true;
                        value    = expression;
                        //operator = "LIKE";
                        operatorType = OperatorType.LIKE;
                    }else{
                        //если следы оператора "LIKE" не обнаружены ==> считаем,
                        //что это оператор равенства
                        operator = "=";
                        value    = expression;
                        operatorType = operatorType.SIMPLE;
                    }
                }
                if (values == null)
                    value = convertStringToValueType(value);
                else {
                    Object[] tempValues = new Object[values.length];
                    for (int i=0; i<values.length; ++i)
                        tempValues[i] = convertStringToValueType(values[i]);
                    value = tempValues;
                }
            }
        }
        this.expression=expression;
        if (log.isDebugEnabled())
            log.debug(
                    "Expression seted. Operator: "+operator+"; Value: "+value);
    }

    private static String[] extractValuesFromList(String listString)
        throws QueryFilterElementException
    {
        for (String separator: LIST_SEPARATORS)
            if (listString.contains(separator))
                return listString.trim().split("\\s*"+separator+"\\s*");
        return null;
    }

    private Object convertStringToValueType(Object value)
        throws QueryFilterElementException
    {
        try{
            return
                converter.convert(
                    propertyClass
                    , value
                    , pattern == null?
                        propertyDescriptor.getPattern() : pattern);
        }catch(ValueTypeConverterException e){
            throw new QueryFilterElementException(
                messages.format(
                    "ExpressionError"
                    , expression, propertyDescriptor.getDisplayName())
                , e);
        }
    }

    public void setPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        this.propertyDescriptor=propertyDescriptor;
        this.propertyClass=propertyDescriptor.getPropertyClass();
    }

    public PropertyDescriptor getPropertyDescriptor() {
        return propertyDescriptor;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseAliasAsProperty() {
        return useAliasAsProperty;
    }

    public void setUseAliasAsProperty(boolean useAliasAsProperty) {
        this.useAliasAsProperty = useAliasAsProperty;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public OperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(OperatorType operatorType) {
        this.operatorType = operatorType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

}
