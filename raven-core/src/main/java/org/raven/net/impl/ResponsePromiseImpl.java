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
package org.raven.net.impl;

import org.raven.net.Response;
import org.raven.net.ResponsePromise;
import org.raven.net.ResponseReadyCallback;

/**
 *
 * @author Mikhail Titov
 */
public class ResponsePromiseImpl extends Response.DummyResponse implements ResponsePromise {
    private Response response;
    private Throwable error;
    private ResponseReadyCallback callback;

    public ResponsePromiseImpl() {
        super(null);
    }
    
    public void success(Response res) {
        ResponseReadyCallback _callback = null;
        synchronized(this) {
            this.response = res;
            _callback = callback;
        }
        if (_callback!=null)
            _callback.onSuccess(response);
    }
    
    public void error(Throwable ex) {
        ResponseReadyCallback _callback = null;
        synchronized(this) {
            this.error = ex;
            _callback = callback;
        }
        if (_callback!=null)
            _callback.onError(error);
    }

    public void onComplete(ResponseReadyCallback callback) {
        Response _response = null;
        Throwable _error = null;
        synchronized(this) {
            if (response==null && error==null)
                this.callback = callback;
            else {
                _response = response;
                _error = error;
            }
        }
        if      (_response!=null)
            callback.onSuccess(response);
        else if (_error!=null)
            callback.onError(error);                
    }
    
}
