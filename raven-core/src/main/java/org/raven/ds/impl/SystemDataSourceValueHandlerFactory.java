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

package org.raven.ds.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
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
public class SystemDataSourceValueHandlerFactory implements AttributeValueHandlerFactory
{
    public final static String TYPE = "SystemDataSource";
    
    private final NodePathResolver pathResolver;

    public SystemDataSourceValueHandlerFactory(NodePathResolver pathResolver)
    {
        this.pathResolver = pathResolver;
    }
    
    public AttributeValueHandler createValueHandler(NodeAttribute attribute)
    {
        try
        {
            return new SystemDataSourceValueHandler(attribute);
        } 
        catch (InvalidPathException ex)
        {
            throw new RavenRuntimeException(String.format(
                    "Error creating reference to system datasource value handler for attribute (%s)"
                    , pathResolver.getAbsolutePath(attribute))
                    , ex);
        }
    }

    public String getName()
    {
        return "System Data Source";
    }

}
