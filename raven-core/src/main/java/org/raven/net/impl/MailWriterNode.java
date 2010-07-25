/*
 *  Copyright 2010 Mikhail Titov.
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

package org.raven.net.impl;

import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.ds.DataContext;
import org.raven.ds.DataSource;
import org.raven.ds.impl.AbstractSafeDataPipe;
import org.raven.expr.BindingSupport;
import org.raven.log.LogLevel;
import org.raven.net.MailMessagePart;
import org.raven.tree.Node;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={AttributeValueMessagePartNode.class})
public class MailWriterNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter
    private String smptHost;

    @NotNull @Parameter(defaultValue="25")
    private Integer smptPort;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useSsl;

    @NotNull @Parameter(defaultValue="false")
    private Boolean useAuth;

    @Parameter
    private String user;

    @Parameter
    private String password;

    @NotNull @Parameter
    private String subject;

    @NotNull @Parameter(defaultValue="utf-8")
    private String subjectEncoding;

    @NotNull @Parameter(defaultValue="utf-8")
    private String contentEncoding;

    @NotNull @Parameter
    private String from;

    @NotNull @Parameter
    private String to;

    static {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) throws Exception
    {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", smptPort);
        if (useAuth)
            props.put("mail.smtp.auth", "true");
        if (useSsl){
            props.put("mail.smtp.socketFactory.port", 465);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            props.put("mail.debug", "true");

        Session session = null;
        if (useAuth)
            session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            });
        else
            session = Session.getDefaultInstance(props);

        if (isLogLevelEnabled(LogLevel.DEBUG))
            session.setDebug(true);

        try{
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            String[] addrs = to.split("\\s*,\\s*");
            InternetAddress[] toAddresses = new InternetAddress[addrs.length];
            for (int i=0; i<addrs.length; ++i)
                toAddresses[i] = new InternetAddress(addrs[i]);

            msg.setRecipients(Message.RecipientType.TO, toAddresses);
            msg.setSubject(subject, subjectEncoding);
    //        msg.setText(body.toString(), bodyEncoding, "plain");
            msg.setHeader("X-Mailer", "Raven-Monitoring");

            createContent(msg);

            msg.setSentDate(new Date());

            Transport.send(msg);
        }finally{
            bindingSupport.reset();
        }
    }

    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    private void createContent(MimeMessage message) throws Exception
    {
        List<MailMessagePart> parts = new ArrayList<MailMessagePart>();
        List<Node> childs = getSortedChildrens();
        if (childs!=null && !childs.isEmpty())
            for (Node child: childs)
                if (Status.STARTED.equals(getStatus()) && child instanceof MailMessagePart)
                    parts.add((MailMessagePart)child);
            
        if (parts.isEmpty())
            throw new Exception("Nothing to send. The message must contains at least one message part");

        Multipart multipart = new MimeMultipart();
        for (MailMessagePart part: parts){
            MimeBodyPart bodyPart = new MimeBodyPart();
            setContent(bodyPart, part);
            if (part.getFileName()!=null)
                bodyPart.setFileName(MimeUtility.encodeText(part.getFileName(), getSubjectEncoding(), null));
            multipart.addBodyPart(bodyPart);
        }
        message.setContent(multipart);
    }

    private void setContent(MimeBodyPart bodyPart, MailMessagePart part) throws MessagingException
    {
        byte[] is = converter.convert(byte[].class, part.getContent(), getContentEncoding());
        ByteArrayMailDataSource ds = new ByteArrayMailDataSource(is, part.getContentType(), part.getName());
        bodyPart.setDataHandler(new DataHandler(ds));
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmptHost() {
        return smptHost;
    }

    public void setSmptHost(String smptHost) {
        this.smptHost = smptHost;
    }

    public Integer getSmptPort() {
        return smptPort;
    }

    public void setSmptPort(Integer smptPort) {
        this.smptPort = smptPort;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubjectEncoding() {
        return subjectEncoding;
    }

    public void setSubjectEncoding(String subjectEncoding) {
        this.subjectEncoding = subjectEncoding;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Boolean getUseAuth() {
        return useAuth;
    }

    public void setUseAuth(Boolean useAuth) {
        this.useAuth = useAuth;
    }

    public Boolean getUseSsl() {
        return useSsl;
    }

    public void setUseSsl(Boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }
}
