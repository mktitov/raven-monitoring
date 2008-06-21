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

package org.raven.tree;

/**
 * The value handler of the attribute. 
 * 
 * @author Mikhail Titov
 */
public interface AttributeValueHandler 
{
    /**
     * Adds listener.
     */
    public void addListener(AttributeValueHandlerListener listener);
    /**
     * Removes listener.
     */
    public void removeListener(AttributeValueHandlerListener listener);
    /**
     * Sets the value that must be handled. Must be call before {@link #handleData()}.
     * @see #handleData() 
     */
    public void setData(String value) throws Exception;
    /**
     * Returns the string value. 
     */
    public String getData();
    /**
     * Returns the translated value.
     */
    public Object handleData();
    /**
     * Closes the value handler. The value handler can't be use after this method call.
     */
    public void close();
    /**
     * If returns <b>true</b> then method {@link #handleData()} can be used.
     */
//    public boolean canHandleValue();
    /**
     * If returns <b>false</b> the method {@link NodeAttribute#getReferenceValues()} must returns
     * <b>null</b>
     */
    public boolean isReferenceValuesSupported();
    /**
     * If returns <b>true</b> method {@link NodeAttribute#isExpression()} must returns <b>true</b>.
     */
    public boolean isExpressionSupported();
    /**
     * Returns <b>true</b> if expression in the value handler is valid
     * @return
     */
    public boolean isExpressionValid();
}
