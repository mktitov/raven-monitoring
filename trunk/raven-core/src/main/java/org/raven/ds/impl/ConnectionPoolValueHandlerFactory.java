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

import org.raven.RavenRuntimeException;
import org.raven.tree.AttributeValueHandler;
import org.raven.tree.AttributeValueHandlerFactory;
import org.raven.tree.InvalidPathException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodePathResolver;

/**
 *
 * @author Mikhail Titov
 */
public class ConnectionPoolValueHandlerFactory implements AttributeValueHandlerFactory
{
    public final static String TYPE = "ConnectionPool";

    private final NodePathResolver pathResolver;

    public ConnectionPoolValueHandlerFactory(NodePathResolver pathResolver)
    {
        this.pathResolver = pathResolver;
    }

    public AttributeValueHandler createValueHandler(NodeAttribute attribute)
    {
        try
        {
            return new ConnectionPoolValueHandler(attribute);
        }
        catch (InvalidPathException ex)
        {
            throw new RavenRuntimeException(String.format(
                    "Error creating reference to the connection pool value handler " +
                    "for attribute (%s)"
                    , pathResolver.getAbsolutePath(attribute))
                    , ex);
        }
    }

    public String getName()
    {
        return "Connection pool";
    }
}
