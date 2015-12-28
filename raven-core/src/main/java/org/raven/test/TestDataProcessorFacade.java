/*
 * Copyright 2015 Mikhail Titov.
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
package org.raven.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.raven.dp.DataProcessor;
import org.raven.dp.DataProcessorFacade;
import org.raven.dp.impl.DataProcessorFacadeConfig;
import org.raven.dp.impl.DataProcessorFacadeImpl;
import org.weda.beans.ObjectUtils;

/**
 *
 * @author Mikhail Titov
 */
public class TestDataProcessorFacade extends DataProcessorFacadeImpl {
    private volatile Object waitingForMessage = DataProcessor.VOID;
    private volatile DataProcessorFacade messageSender = null;
    private volatile CountDownLatch latch;

    public TestDataProcessorFacade(DataProcessorFacadeConfig config) {
        super(config);
    }

    @Override
    public boolean send(Object message) {
        Object mess; DataProcessorFacade sender;
        if (message instanceof MessageFromFacade) {
            mess = ((MessageFromFacade)message).message;
            sender = ((MessageFromFacade)message).facade;
        } else {
            mess = message;
            sender = null;
        }
        if (ObjectUtils.equals(mess, waitingForMessage) && (messageSender==null || ObjectUtils.equals(sender, messageSender))) {
            if (latch!=null)
                latch.countDown();
        }
        return super.send(message); 
    }
    
    public void setWaitForMessage(Object mess, DataProcessorFacade sender) {
        latch = new CountDownLatch(1);
        this.waitingForMessage = mess;
        this.messageSender = sender;
    }
    
    public void setWaitForMessage(Object mess) {
        setWaitForMessage(mess, null);
    }
    
    public boolean waitForMessage() throws InterruptedException {
        latch.await();
        return true;
    }
    
    public boolean waitForMessage(long timeout) throws InterruptedException {
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }
    
    public boolean waitForMessage(Object mess) throws InterruptedException {
        latch = new CountDownLatch(1);
        waitingForMessage = mess;
        latch.await();
        return true;
    }
    
    public boolean waitForMessage(Object mess, long timeout) throws InterruptedException {
        latch = new CountDownLatch(1);
        waitingForMessage = mess;
        return latch.await(timeout, TimeUnit.MILLISECONDS);
    }
}
