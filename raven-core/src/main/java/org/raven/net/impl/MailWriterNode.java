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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import javax.activation.DataHandler;
import javax.mail.Message;
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
import org.raven.expr.impl.IfNode;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.log.LogLevel;
import org.raven.net.MailMessagePart;
import org.raven.util.NodeUtils;
import org.weda.annotations.constraints.NotNull;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(childNodes={
    AttributeValueMessagePartNode.class, ViewableObjectsMessagePartNode.class, IfNode.class})
public class MailWriterNode extends AbstractSafeDataPipe
{
    @NotNull @Parameter
    private String smtpHost;

    @NotNull @Parameter(defaultValue="25")
    private Integer smtpPort;

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

    @Parameter
    private String fromPersonalName;

    @NotNull @Parameter(defaultValue="utf-8")
    private String fromEncoding;

    @NotNull @Parameter
    private String to;

    @NotNull @Parameter(defaultValue="30000")
    private Integer connectionTimeout;

    @NotNull @Parameter(defaultValue="30000")
    private Integer timeout;
    
    @Parameter(valueHandlerType=ScriptAttributeValueHandlerFactory.TYPE)
    private Object errorHandler;
    
    @NotNull @Parameter(defaultValue="false")
    private Boolean useErrorHandler;
    
    static {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }

    @Override
    protected void doSetData(DataSource dataSource, Object data, DataContext context) 
    {
        if (data==null){
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("Recieved NULL data. Skiping");
            sendDataToConsumers(null, context);
            return;
        }

        if (isLogLevelEnabled(LogLevel.DEBUG))
            getLogger().debug("Creating mail for data: {}", data);
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        if (useAuth)
            props.put("mail.smtp.auth", "true");
        if (useSsl){
            props.put("mail.smtp.socketFactory.port", 465);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        if (isLogLevelEnabled(LogLevel.DEBUG))
            props.put("mail.debug", "true");
        
        try {
            Session session = null;
            if (useAuth) {
                session = Session.getInstance(props, new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });
            } else 
                session = Session.getDefaultInstance(props);
            
            if (isLogLevelEnabled(LogLevel.DEBUG)) 
                session.setDebug(true);
            
            try {
                bindingSupport.put(DATA_BINDING, data);
                bindingSupport.put(DATA_CONTEXT_BINDING, context);
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(from, fromPersonalName, fromEncoding));
                String[] addrs = to.split("\\s*,\\s*");
                InternetAddress[] toAddresses = new InternetAddress[addrs.length];
                for (int i = 0; i < addrs.length; ++i) {
                    toAddresses[i] = new InternetAddress(addrs[i]);
                }
                
                msg.setRecipients(Message.RecipientType.TO, toAddresses);
                msg.setSubject(subject, subjectEncoding);
                msg.setHeader("X-Mailer", "Raven-Monitoring");
                
                createContent(msg, context);
                
                msg.setSentDate(new Date());
                
                Transport.send(msg);
            } finally {
                bindingSupport.reset();
            }
            sendDataToConsumers(data, context);
        } catch(Throwable e) {
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error("Error sending email mesage", e);
            sendDataToConsumers(processError(data, context, e), context);
        }
    } 

    private Object processError(Object data, DataContext context, Throwable e) 
    {
        if (!useErrorHandler)
            return data;
        try {
            bindingSupport.put(DATA_CONTEXT_BINDING, context);
            bindingSupport.put(DATA_BINDING, data);
            bindingSupport.put(EXCEPTION_BINDING, e);
            return errorHandler;
        } finally {
            bindingSupport.reset();
        }
    }
    
    @Override
    protected void doAddBindingsForExpression(
            DataSource dataSource, Object data, DataContext context, BindingSupport bindingSupport)
    {
    }

    private void createContent(MimeMessage message, DataContext context) throws Exception
    {
        List<MailMessagePart> parts = NodeUtils.getEffectiveChildsOfType(
                this, MailMessagePart.class);

        if (parts.isEmpty())
            throw new Exception(
                    "Nothing to send. The message must contains at least one message part");

        Multipart multipart = new MimeMultipart();
        for (MailMessagePart part: parts){
            MimeBodyPart bodyPart = new MimeBodyPart();
            setContent(bodyPart, part, context);
            if (part.getFileName()!=null)
                bodyPart.setFileName(MimeUtility.encodeText(
                        part.getFileName(), getSubjectEncoding(), null));
            multipart.addBodyPart(bodyPart);
        }
        message.setContent(multipart);
    }

    private void setContent(MimeBodyPart bodyPart, MailMessagePart part, DataContext context) 
            throws Exception
    {
        javax.activation.DataSource ds = null;
        Object content = part.getContent(context);
        if (content instanceof String && part.getContentType().startsWith("text")) {
            String subtype = part.getContentType().split("/")[1];
            bodyPart.setText((String) content, getContentEncoding(), subtype);
        } else {
            if (content instanceof javax.activation.DataSource)
                ds = new DataSourceWrapper(ds, part.getContentType(), part.getName());
            else {
                byte[] is = converter.convert(byte[].class, content, getContentEncoding());
                ds = new ByteArrayDataSource(is, part.getContentType(), part.getName());
            }
            bodyPart.setDataHandler(new DataHandler(ds));
        }
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromEncoding() {
        return fromEncoding;
    }

    public void setFromEncoding(String fromEncoding) {
        this.fromEncoding = fromEncoding;
    }

    public String getFromPersonalName() {
        return fromPersonalName;
    }

    public void setFromPersonalName(String fromPersonalName) {
        this.fromPersonalName = fromPersonalName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smptHost) {
        this.smtpHost = smptHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smptPort) {
        this.smtpPort = smptPort;
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

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Object getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(Object errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Boolean getUseErrorHandler() {
        return useErrorHandler;
    }

    public void setUseErrorHandler(Boolean useErrorHandler) {
        this.useErrorHandler = useErrorHandler;
    }
}
