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
package org.raven.net.impl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.net.NettyByteBufferAllocator;
import org.raven.tree.impl.BaseNode;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass
public class NettyByteBufferAllocatorNode extends BaseNode implements NettyByteBufferAllocator {
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean useBuffersPool;
    
    @NotNull @Parameter(defaultValue = "false")
    private Boolean preferDirectBuffer;
    
    private volatile ByteBufAllocator bufferAllocator;

    @Override
    protected void doInit() throws Exception {
        super.doInit(); 
        bufferAllocator = null;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        bufferAllocator = useBuffersPool? 
                new PooledByteBufAllocator(preferDirectBuffer) : 
                new UnpooledByteBufAllocator(preferDirectBuffer);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop(); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public ByteBufAllocator getByteBufferAllocator() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Boolean getUseBuffersPool() {
        return useBuffersPool;
    }

    public void setUseBuffersPool(Boolean useBuffersPool) {
        this.useBuffersPool = useBuffersPool;
    }

    public Boolean getPreferDirectBuffer() {
        return preferDirectBuffer;
    }

    public void setPreferDirectBuffer(Boolean preferDirectBuffer) {
        this.preferDirectBuffer = preferDirectBuffer;
    }    
}
