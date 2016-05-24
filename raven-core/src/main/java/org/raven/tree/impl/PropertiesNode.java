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
package org.raven.tree.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.tree.DataFile;
import org.raven.tree.DataFileException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.weda.annotations.constraints.NotNull;
import org.weda.beans.ObjectUtils;

/**
 * Node that holds a properties
 * @author Mikhail Titov 
 */
//TODO add documentation and node icon
@NodeClass
public class PropertiesNode extends BaseNode implements Viewable {
    @NotNull @Parameter(valueHandlerType=DataFileValueHandlerFactory.TYPE)
    private DataFile propertiesFile;

    @NotNull @Parameter(defaultValue="true")
    private Boolean cacheEnabled;
    
    private volatile Properties cachedProperties;
    private volatile Long cachedPropertiesChecksum;

    @Override
    protected void initFields() {
        super.initFields();
        cachedProperties = null;
        cachedPropertiesChecksum = null;
    }

    public DataFile getPropertiesFile() {
        return propertiesFile;
    }

    public void setPropertiesFile(DataFile propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public Boolean getCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(Boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
    
    public Properties getProperties() throws DataFileException, IOException {
        if (!isStarted()) 
            return null;
        if (!cacheEnabled)
            return readProperties();
        if (cachedProperties==null || !ObjectUtils.equals(cachedPropertiesChecksum, propertiesFile.getChecksum())) {
            synchronized(this) {
                if (cachedProperties==null || !ObjectUtils.equals(cachedPropertiesChecksum, propertiesFile.getChecksum())) {
                    cachedProperties = readProperties();
                    cachedPropertiesChecksum = propertiesFile.getChecksum();
                }
            }
        }
        return cachedProperties;
    }
    
    private Properties readProperties() throws IOException, DataFileException {
        Properties _props = new Properties();
        if (propertiesFile.getFileSize()>0) 
            _props.load(propertiesFile.getDataReader());
        return _props;
    }

    @Override
    public Map<String, NodeAttribute> getRefreshAttributes() throws Exception {
        return null;
    }

    @Override
    public List<ViewableObject> getViewableObjects(Map<String, NodeAttribute> refreshAttributes) throws Exception {
        if (!isStarted() || propertiesFile.getMimeType()==null || propertiesFile.getMimeType().isEmpty())
            return null;
        ViewableObject obj = new DataFileViewableObject(propertiesFile, this);
        return Arrays.asList(obj);
    }

    @Override
    public Boolean getAutoRefresh() {
        return true;
    }
}
