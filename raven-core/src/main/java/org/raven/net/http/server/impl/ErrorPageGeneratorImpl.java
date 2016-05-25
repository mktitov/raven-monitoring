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

import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.raven.expr.impl.RavenScriptTemplateEngine;
import org.raven.net.http.server.ErrorPageGenerator;
import org.raven.tree.ResourceManager;
import org.raven.tree.impl.FileNode;
import org.raven.tree.impl.PropertiesNode;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class ErrorPageGeneratorImpl implements ErrorPageGenerator {
    private final ResourceManager resourceManager;
    private final String pageTemplatePath;
    private final String messagesResourceKey;
    private volatile Template template;
    private volatile Long lastPageCheckSum;

    public ErrorPageGeneratorImpl(ResourceManager resourceManager, String pageTemplateResourceKey, String messagesResourceKey) {        
        this.resourceManager = resourceManager;
        this.pageTemplatePath = pageTemplateResourceKey;
        this.messagesResourceKey = messagesResourceKey;
    }
    
    public String buildPage(Map<String, Object> bindings, Locale locale) throws Exception {
        FileNode pageNode = (FileNode) resourceManager.getResource(pageTemplatePath, Locale.ENGLISH);
        if (pageNode==null)
            throw new Exception("Not found page template: "+pageTemplatePath);
        PropertiesNode messages = (PropertiesNode) resourceManager.getResource(messagesResourceKey, locale);
        if (messages==null)
            throw new Exception(String.format("Not found message bundle (%s) for locale (%s)", messagesResourceKey, locale));
        if (template==null || !ObjectUtils.equals(lastPageCheckSum, pageNode.getFile().getChecksum())) {
            synchronized(this) {                
                if (template==null || !ObjectUtils.equals(lastPageCheckSum, pageNode.getFile().getChecksum())) {
                    template = new SimpleTemplateEngine(true).createTemplate(pageNode.getFile().getDataReader());
                    template = new RavenScriptTemplateEngine(true).createTemplate(pageNode.getFile().getDataReader());
                    lastPageCheckSum = pageNode.getFile().getChecksum();
                }
            }
        }
        Map<String, Object> _bindings = new HashMap<>(bindings);
        _bindings.put("messages", messages.getProperties());
        return template.make(_bindings).toString();
    }
}
