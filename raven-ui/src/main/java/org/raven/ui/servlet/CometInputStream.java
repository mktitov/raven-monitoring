/*
 * Copyright 2014 Mikhail Titov.
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

package org.raven.ui.servlet;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Mikhail Titov
 */
public abstract class CometInputStream extends InputStream implements DataReceiver {
    public final static CometInputStream EMPTY_STREAM = new CometInputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }

        public boolean canPushBuffer() {
            return false;
        }

        public void pushBuffer(ByteBuf buf) {
        }

        public void dataStreamClosed() {
        }
    };
}
