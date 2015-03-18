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
package org.raven.ds.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import org.junit.Assert;
import org.raven.ds.AskCallback;

public class RavenFutureImplTest extends Assert {

    @Test
    public void successCallbackTest() {
        AskCallback callback = createMock(AskCallback.class);
        callback.onSuccess("test");
        replay(callback);
        
        RavenFutureImpl future = new RavenFutureImpl(callback);
        future.set("test");
        
        verify(callback);
    }
    
    @Test
    public void errorCallbackTest() {
        AskCallback callback = createMock(AskCallback.class);
        Exception error = new Exception("test");
        callback.onError(error);
        replay(callback);
        
        RavenFutureImpl future = new RavenFutureImpl(callback);
        future.setError(error);
        
        verify(callback);
    }
    
    @Test
    public void cancelCallbackTest() {
        AskCallback callback = createMock(AskCallback.class);
        callback.onError(isA(FutureCanceledException.class));
        replay(callback);
        
        RavenFutureImpl future = new RavenFutureImpl(callback);
        future.cancel(true);
        
        verify(callback);
    }
    
    @Test
    public void getSuccesTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, "test", 0);
        assertEquals("test", future.get());
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }
    
    @Test(expected = ExecutionException.class)
    public void getErrorTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        setFutureValue(future, new Exception("test"), 0);
        try {
            future.get();
        } finally {
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }
    
    @Test(expected = FutureCanceledException.class)
    public void getCanceledTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        cancelFuture(future, 10);
        try {
            future.get();
        } finally {
            assertTrue(future.isDone());
            assertTrue(future.isCancelled());
        }
    }
    
    @Test
    public void getSuccessWithTimeoutTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, "test", 40);
        assertEquals("test", future.get(50, TimeUnit.MILLISECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }
    
    @Test(expected = TimeoutException.class)
    public void getTimeoutExWithTimeoutTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, "test", 60);
        assertEquals("test", future.get(50, TimeUnit.MILLISECONDS));
    }
    
    @Test
    public void getOrElseSuccesTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, "test", 0);
        assertEquals("test", future.getOrElse("error"));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }
    
    @Test
    public void getOrElseErrorTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, new Exception("test"), 0);
        assertEquals("error", future.getOrElse("error"));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }
    
    @Test
    public void getOrElseCancelTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        cancelFuture(future, 0);
        assertEquals("error", future.getOrElse("error"));
        assertTrue(future.isDone());
        assertTrue(future.isCancelled());
    }
    
    @Test
    public void getOrElseSuccessWithTimeoutTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, "test", 40);
        assertEquals("test", future.getOrElse("error", 50, TimeUnit.MILLISECONDS));
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }  
    
    @Test
    public void getOrElseTimeoutWithTimeoutTest() throws Exception {
        RavenFutureImpl future = new RavenFutureImpl();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
        setFutureValue(future, "test", 60);
        assertEquals("error", future.getOrElse("error", 50, TimeUnit.MILLISECONDS));
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());
    }  
    
    private void setFutureValue(final RavenFutureImpl future, final Object value, final long sleepBeforeSet) {
        new Thread(new Runnable() {
            public void run() {
                if (sleepBeforeSet>0)
                    try {
                        Thread.sleep(sleepBeforeSet);
                    } catch (InterruptedException ex) {
                    }
                if (value instanceof Throwable)
                    future.setError((Throwable)value);
                else 
                    future.set(value);
            }
        }).start();
    }
    
    private void cancelFuture(final RavenFutureImpl future, final long sleepBeforeSet) {
        new Thread(new Runnable() {
            public void run() {
                if (sleepBeforeSet>0)
                    try {
                        Thread.sleep(sleepBeforeSet);
                    } catch (InterruptedException ex) {
                    }
                future.cancel(true);
            }
        }).start();
    }
    
}
