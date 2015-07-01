/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.server.app.service.impl;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.raven.server.app.service.IconResolver;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class IconResolverImpl implements IconResolver
{
    public String getPath(Class<? extends Node> nodeClass)
    {
        Class clazz = nodeClass;
        while (clazz!=null && Node.class.isAssignableFrom(clazz)) {
            String path = clazz.getCanonicalName().replace('.', '/')+".png";
            if (getClass().getClassLoader().getResource(path)!=null)
                return path;
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    public byte[] getIcon(String path)
    {
        InputStream iconStream = getClass().getClassLoader().getResourceAsStream(path);
        if (iconStream==null)
            return null;
        try{
            try {
                return IOUtils.toByteArray(iconStream);
            } catch (IOException ex) {
                return null;
            }
        } finally {
            IOUtils.closeQuietly(iconStream);
        }
    }
}
