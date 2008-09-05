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

import org.raven.ds.impl.*;
import java.util.concurrent.TimeUnit;
import org.raven.annotations.Parameter;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
public class ScannerNode extends DataPipeImpl
{
    @Parameter(defaultValue="5")
    @NotNull
    private Integer threadCount;

    @Parameter()
    @NotNull
    private String ipRanges;

    @Parameter()
    @NotNull
    private Integer interval;

    @Parameter()
    @NotNull
    private TimeUnit intervalUnit;

    @Parameter(defaultValue="true")
    @NotNull()
    private Boolean ipAddressFilter;

    @Parameter(defaultValue="host")
    @NotNull()
    private String hostAttributeName;

}
