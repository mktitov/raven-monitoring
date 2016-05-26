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
package org.raven.net.http.server.impl;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.raven.net.http.server.HttpConsts;
import org.raven.test.RavenCoreTestCase;
import org.raven.tree.ResourceManager;

/**
 *
 * @author Mikhail Titov
 */
public class ErrorPageGeneratorImplTest extends RavenCoreTestCase {
    private ResourceManager resourceManager;
    
    @Before
    public void prepare() {
        resourceManager = registry.getService(ResourceManager.class);        
    }
    
    @Test
    public void test() throws Exception {
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("projectName", "test project");
        bindings.put("statusCode", "404");
        bindings.put("statusCodeDesc", "NOT FOUND");
        bindings.put("message", null);
        bindings.put("exceptions", Arrays.asList(new Exception("Root exception"), new Exception("Cause exception")));
        bindings.put("requestURL", "http://localhost:8080/projects/test");
        bindings.put("responseBuilderNodePath", "/Projects/Test");
        bindings.put("queryString", "?a=1&b=2");
        bindings.put("devMode", true);
        bindings.put("headers", createHeaders());
        bindings.put("parameters", createParameters());
        
        ErrorPageGeneratorImpl generator = new ErrorPageGeneratorImpl(
                resourceManager, HttpConsts.ERROR_PAGE_RESOURCE, HttpConsts.ERROR_PAGE_MESSAGES_RESOURCE);
        String res = generator.buildPage(bindings, new Locale("ru", "RU"), true);
        System.out.println("HTML: \n"+res);
        
        FileUtils.write(new File("target/error.html"), res, Charset.forName("UTF-8"));
    }
    
    private Map<String, String> createHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("header1", "header value1");
        headers.put("header2", "header value2");
        return headers;
    }
    
    private Map<String, String> createParameters() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "2");
        return params;
    }
}
