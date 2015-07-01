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
package org.raven.net.impl;

import java.io.File;
import java.util.Map;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.net.ContextUnavailableException;
import org.raven.net.NetworkResponseServiceExeption;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=NetworkResponseServiceNode.class)
public class FSDirectoryResourceContext extends AbstractNetworkResponseContext {
    @NotNull @Parameter
    private String directory;
    
    @Override
    public Object doGetResponse(String requesterIp, Map<String, Object> params) throws 
        NetworkResponseServiceExeption 
    {
        String subpath = (String) params.get(NetworkResponseServiceNode.SUBCONTEXT_PARAM);
        File file = new File(directory+File.separator+subpath);
        if (!file.exists() || !file.isFile())
            throw new ContextUnavailableException("File not found: "+subpath);
//        File
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
