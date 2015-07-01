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

import java.io.InputStream;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public interface DynamicImageNode extends Node
{
    /**
     * Returns the image format.
     */
    public ImageFormat getImageFormat();
    /**
     * Returns the image width.
     */
    public Integer getWidth();
    /**
     * Returns the image height.
     */
    public Integer getHeight();
    /**
     * Generates the image and returns the input stream of this image.
     */
    public InputStream render();
}
