/*
 * Copyright 2013 Mikhail Titov.
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
package org.raven.ds;

import java.io.IOException;
import javax.activation.DataSource;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.ViewableObject;

/**
 *
 * @author Mikhail Titov
 */
public class DataSourceViewableObject implements ViewableObject {
    private final javax.activation.DataSource dataSource;
    private final String mimeType;
    private final Node owner;

    public DataSourceViewableObject(DataSource dataSource, String mimeType, Node owner) {
        this.dataSource = dataSource;
        this.mimeType = mimeType;
        this.owner = owner;
    }


    public String getMimeType() {
        return mimeType;
    }

    public Object getData() {
        try {
            return dataSource.getInputStream();
        } catch (IOException ex) {
            if (owner.isLogLevelEnabled(LogLevel.ERROR)) 
                owner.getLogger().error("Error extracting input stream from dataSource", ex);
            return null;
        }
    }

    public boolean cacheData() {
        return false;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }
}
