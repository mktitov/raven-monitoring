/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.tree.store.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.weda.internal.annotations.Service;
import org.weda.internal.impl.MessageComposer;
import org.weda.internal.services.MessagesRegistry;

/**
 *
 * @author Mikhail Titov
 */
public class StoreUtils
{
    @Service
    private static MessagesRegistry messagesRegistry;

    private StoreUtils()
    {
    }

    public static String messageComposerToString(MessageComposer composer)
    {
        if (composer==null)
            return null;

        StringBuilder builder = new StringBuilder();
        for (String message: composer.getMessages())
        {
            if (builder.length()>0)
                builder.append(',');
            builder.append(StringEscapeUtils.escapeCsv(message));
        }
        return builder.toString();
    }

    public static MessageComposer stringToMessageComposer(String str)
    {
        if (str==null)
            return null;

        MessageComposer composer = new MessageComposer(messagesRegistry);
        StrTokenizer tokenizer = new StrTokenizer(str, ',', '"');
        while (tokenizer.hasNext())
            composer.append(tokenizer.nextToken());

        return composer;
    }
}
