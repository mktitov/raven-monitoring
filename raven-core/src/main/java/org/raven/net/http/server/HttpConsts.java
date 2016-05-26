/*
 * Copyright 2016 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven.net.http.server;

/**
 *
 * @author Mikhail Titov
 */
public interface HttpConsts {
    public static final String HTTP_SERVER_HEADER = "Raven/1.1";
    public static final String RESOURCES_BASE = "HTTP Server/";
    public static final String ERROR_PAGE_RESOURCE = RESOURCES_BASE+"pages/error_page";
    public static final String ERROR_PAGE_MESSAGES_RESOURCE = RESOURCES_BASE+"messages/messages";
//    public static final String PAGES_RESOURCES_BASE = RESOURCES_BASE+"Pages/";
//    public static final String MESSAGES_RESOURCES_BASE = RESOURCES_BASE+"Messages/";
    public static final String DEFAULT_CONTENT_CHARSET = "UTF-8";
    public static final String SESSIONID_COOKIE_NAME = "RSESSION";
    public static final String FORM_URLENCODED_MIME_TYPE = "application/x-www-form-urlencoded";   
}
