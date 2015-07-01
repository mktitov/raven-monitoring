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

package org.raven.server.app.rest;

import java.net.URLDecoder;
import javax.ws.rs.core.Response;
import org.raven.conf.Configurator;
import org.weda.internal.annotations.Service;

/**
 *
 * @author Mikhail Titov
 */
public class RestHelper
{
    @Service
    private static Configurator configurator;

    public static String decodeParam(String value, String defaultValue) throws Exception
    {
        value = value==null? null : value.trim();
        if (value!=null && value.isEmpty())
            value = null;
        if (value==null)
            return defaultValue;
        return URLDecoder.decode(value, configurator.getConfig().getStringProperty(
                Configurator.REST_ENCODING, "utf-8"));
    }

    public static String decodeAndCheckParam(String value, String defaultValue, String errorMessage)
            throws Exception
    {
        value = decodeParam(value, defaultValue);
        if (value==null)
            throw new Exception(errorMessage);
        return value;
    }

    public static Response badRequest(String message)
    {
        return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
    }
}
