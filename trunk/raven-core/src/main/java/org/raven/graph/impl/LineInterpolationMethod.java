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

package org.raven.graph.impl;

import org.jrobin.data.LinearInterpolator;

/**
 * The wrapper
 * @author Mikhail Titov
 */
public enum LineInterpolationMethod
{
    INTERPOLATE_LEFT(LinearInterpolator.INTERPOLATE_LEFT),
    INTERPOLATE_RIGHT(LinearInterpolator.INTERPOLATE_RIGHT),
    INTERPOLATE_LINEAR(LinearInterpolator.INTERPOLATE_LINEAR),
    INTERPOLATE_REGRESSION(LinearInterpolator.INTERPOLATE_REGRESSION);

    private final int id;

    private LineInterpolationMethod(int id)
    {
        this.id = id;
    }

    public int getId()
    {
        return id;
    }
}
