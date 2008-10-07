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

package org.raven.net;

import java.util.regex.Pattern;
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
public class RegexpFilenameFilter implements FilenameFilter
{
    private final Pattern filemaskPattern;
    private final Logger logger;

    public RegexpFilenameFilter(String pattern, Logger logger)
    {
        this.logger = logger;
        filemaskPattern = Pattern.compile(pattern);
    }

    public boolean filter(String filename)
    {
        boolean res = filemaskPattern.matcher(filename).matches();
        
        if (!res && logger.isDebugEnabled())
            logger.debug(String.format(
                    "Ignoring file (%s). Not matches to regexp file mask", filename));
            
        return res;
    }
}
