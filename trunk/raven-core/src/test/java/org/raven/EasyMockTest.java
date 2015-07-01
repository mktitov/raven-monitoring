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
package org.raven;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Mikhail Titov
 */

@RunWith(EasyMockRunner.class)
public class EasyMockTest extends EasyMock {
    public interface ITest {
        public void test();
    }
    
    
    @Test
    public void test() throws InterruptedException {
        final IMocksControl mocks = createControl();
        final ITest test = mocks.createMock(ITest.class);
        
        test.test();
//        test.test();
        mocks.replay();        
        Thread t = new Thread(){
            @Override
            public void run() {
                test.test();
                test.test();
            }
        };
        t.start();
        t.join();
//        test.test();
        mocks.verify();
    }
}
