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
package org.raven.stream;

/**
 *
 * @author Mikhail Titov
 */
public interface DataPacket<T> {
    /**
     * Returns the data which packet contains
     */
    public T getData();
    /**
     * Set's the data for the packet
     * Warning! This method must be call only by data packet owner     * 
     */
    public void setData(T data);
    /**
     * Send's the ack message to the owner
     */
    public void ack();
    /**
     * Send's the reject message to the owner
     */
    public void reject();
    /**
     * Returns the sequence number of the packet
     */
    public long getSeqNum();
    /**
     * Set's the sequence number of the packet
     * @param seqnum 
     */
    public void setSeqNum(long seqnum);
}
