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

import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class ViewableObjectImpl implements ViewableObject
{
    private final String mimeType;
    private final Object data;
    private final boolean cacheData;
    private final int width;
    private final int height;
    private final String toStringValue;

    public ViewableObjectImpl(String mimeType, Object data)
    {
        this(mimeType, data, false);
    }

    public ViewableObjectImpl(String mimeType, Object data, String toStringValue)
    {
        this(mimeType, data, false, toStringValue);
    }

    public ViewableObjectImpl(String mimeType, Object data, boolean cacheData)
    {
        this(mimeType, data, cacheData, 0, 0);
    }

    public ViewableObjectImpl(String mimeType, Object data, boolean cacheData, String toStringValue)
    {
        this(mimeType, data, cacheData, 0, 0, toStringValue);
    }

    public ViewableObjectImpl(
            String mimeType, Object data, boolean cacheData, int width, int height) 
    {
        this(mimeType, data, cacheData, width, height, null);
    }

    public ViewableObjectImpl(
            String mimeType, Object data, boolean cacheData, int width, int height
            , String toStringValue)
    {
        this.mimeType = mimeType;
        this.data = data;
        this.cacheData = cacheData;
        this.width = width;
        this.height = height;
        this.toStringValue = toStringValue;
    }

    public Object getData()
    {
        return data;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public boolean cacheData()
    {
        return cacheData;
    }

    public int getHeight()
    {
        return height;
    }

    public int getWidth()
    {
        return width;
    }

    @Override
    public String toString()
    {
        if (toStringValue!=null)
            return toStringValue;
        if (data!=null)
            return data.toString();
        else
            return super.toString();
    }

}
