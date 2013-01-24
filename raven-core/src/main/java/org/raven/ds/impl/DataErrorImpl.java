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
package org.raven.ds.impl;

import org.raven.ds.DataError;
import org.raven.tree.Node;

/**
 *
 * @author Mikhail Titov
 */
public class DataErrorImpl implements DataError {
    private final Node node;
    private final Throwable error;

    public DataErrorImpl(Node node, Throwable error) {
        this.node = node;
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

    public Node getNode() {
        return node;
    }

    public String getMessage() {
        return error.getMessage();
    }
}
