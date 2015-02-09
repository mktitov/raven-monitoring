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
package org.raven.net.impl;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author Mikhail Titov
 */
public class MailMessage extends MimeMessage {
    private final String messageId;

    public MailMessage(Session session, String messageId) {
        super(session);
        this.messageId = messageId;
    }

    @Override
    protected void updateMessageID() throws MessagingException {
        if (messageId==null)
            super.updateMessageID();
        else
            setHeader("Message-ID", messageId);
    }

}
