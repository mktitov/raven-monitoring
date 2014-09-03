/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import org.raven.VersionService;

/**
 *
 * @author Mikhail Titov
 */
public class VersionServiceImpl implements VersionService {
    private final Collection<String> modulesVersions;

    public VersionServiceImpl(Collection<String> modulesVersions) throws IOException {
        ArrayList<String> versions = new ArrayList<String>();
        versions.addAll(modulesVersions);
        Enumeration<URL> resources = this.getClass().getClassLoader().getResources("raven-module-version.properties");
        while (resources.hasMoreElements()) {
            InputStream stream = resources.nextElement().openStream();
            try {
                Properties props = new Properties();
                props.load(stream);
                versions.add(props.getProperty("version"));
            } finally {
                stream.close();
            }
        }
        this.modulesVersions = versions;
    }

    public Collection<String> getModulesVersion() {
        return modulesVersions;
    }    
}
