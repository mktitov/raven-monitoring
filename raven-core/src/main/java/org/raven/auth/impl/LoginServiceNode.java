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
package org.raven.auth.impl;

import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.Authenticator;
import org.raven.auth.IllegalLoginException;
import org.raven.auth.IllegalPasswordException;
import org.raven.auth.LoginException;
import org.raven.auth.LoginListener;
import org.raven.auth.LoginService;
import org.raven.auth.UserContext;
import org.raven.auth.UserContextConfig;
import org.raven.auth.UserContextConfigurator;
import org.raven.expr.impl.BindingSupportImpl;
import org.raven.log.LogLevel;
import org.raven.tree.Node;
import org.raven.tree.impl.BaseNode;
import static org.raven.util.NodeUtils.*;
import org.raven.util.OperationStatistic;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=LoginManagerNode.class)
public class LoginServiceNode extends BaseNode implements LoginService {
    
    private OperationStatistic loginStat;
    private OperationStatistic authStat;
    private OperationStatistic configureStat;
    private OperationStatistic loginListenersStat;
    
    private BindingSupportImpl bindingSupport;

    public LoginServiceNode() { }

    public LoginServiceNode(String name) {
        super(name);
    }

    @Override
    protected void initFields() {
        super.initFields();
        initStat();
        bindingSupport = new BindingSupportImpl();
    }

    @Override
    public void formExpressionBindings(Bindings bindings) {
        super.formExpressionBindings(bindings);
        bindingSupport.addTo(bindings);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initStat();
        initChildren();
    }
    
    private void initStat() {
        loginStat = new OperationStatistic();
        authStat = new OperationStatistic();
        configureStat = new OperationStatistic();
        loginListenersStat = new OperationStatistic();
    }
    
    protected void initChildren() {
        if (!hasNode(AuthenticatorsNode.NAME))
            addAndStart(new AuthenticatorsNode());
        if (!hasNode(UserContextConfiguratorsNode.NAME))
            addAndStart(new UserContextConfiguratorsNode());
        if (!hasNode(LoginListenersNode.NAME))
            addAndStart(new LoginListenersNode());
		if (!hasNode(ResourcesListNode.NAME))
			addAndStart(new ResourcesListNode());
		if (!hasNode(GroupsListNode.NAME))
			addAndStart(new GroupsListNode());
    }

    @Parameter(readOnly=true)
    public OperationStatistic getLoginStat() {
        return loginStat;
    }

    @Parameter(readOnly=true)
    public OperationStatistic getAuthStat() {
        return authStat;
    }

    @Parameter(readOnly=true)
    public OperationStatistic getConfigureStat() {
        return configureStat;
    }

    @Parameter(readOnly=true)
    public OperationStatistic getLoginListenersStat() {
        return loginListenersStat;
    }
    
    public AuthenticatorsNode getAuthenticatorsNode() {
        return (AuthenticatorsNode) getNode(AuthenticatorsNode.NAME);
    }
    
    public UserContextConfiguratorsNode getUserContextConfiguratorsNode() {
        return (UserContextConfiguratorsNode) getNode(UserContextConfiguratorsNode.NAME);
    }
    
    public GroupsListNode getGroupsNode() {
        return (GroupsListNode) getNode(GroupsListNode.NAME);
    }
    
    private void addAndStart(Node node) {
        addAndSaveChildren(node);
        node.start();
    }
    
    public UserContext login(String login, String password, String host) throws LoginException {
        try {
            if (login==null || login.trim().isEmpty())
                throw new IllegalLoginException();
            if (password==null || password.trim().isEmpty())
                throw new IllegalPasswordException();
            long ts = loginStat.markOperationProcessingStart();
            String authenticator = authenticateUser(login, password);
            UserContext userContext = configureUserContext(
                    new UserContextConfigImpl(authenticator, login, host));
            informLoginListeners(userContext);
            if (isLogLevelEnabled(LogLevel.DEBUG))
                getLogger().debug("User ({}) successfully logged in");
            loginStat.markOperationProcessingEnd(ts);
            return userContext;
        } catch (Throwable e) {
            if (e instanceof LoginException) {
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn("Login failed for user ({}). {}", login, e.getMessage());
                throw (LoginException)e;
            }
            String mess = String.format("Error in login service (%s)", getName());
            if (isLogLevelEnabled(LogLevel.ERROR))
                getLogger().error(mess, e);
            throw new LoginException(mess, e);
        }
    }
    
    private Node getNodeOrThrowEx(String name) throws Exception {
        Node node = getNode(name);
        if (node==null)
            throw new Exception("Node (%s) not found");
        return node;
    }

    private String authenticateUser(String login, String password) throws Exception {
        long ts = authStat.markOperationProcessingStart();
        try {
            boolean debugEnabled = isLogLevelEnabled(LogLevel.DEBUG);
            if (debugEnabled)
                getLogger().debug("Authenticating user ({})", login);
            Node authenticators = getNodeOrThrowEx(AuthenticatorsNode.NAME);
            String authenticator = null;
            for (Authenticator auth: getChildsOfType(authenticators, Authenticator.class)) 
                if (auth.checkAuth(login, password)) {
                    authenticator = auth.getName();
                    break;
                }
            if (authenticator!=null) {
                if (debugEnabled)
                    getLogger().debug("User ({}) successfully authenticated by authenticator ({})"
                            , login, authenticator);
            } else {
                if (isLogLevelEnabled(LogLevel.WARN))
                    getLogger().warn("User ({}) authentication was unsuccessfull", login);
                throw new AuthenticationFailedException(login, getName());
            }
            return authenticator;
        } finally {
            authStat.markOperationProcessingEnd(ts);
        }
    }
    
    private UserContext configureUserContext(UserContextConfig config) throws Exception {
        long ts = configureStat.markOperationProcessingStart();
        try {
            boolean debugEnabled = isLogLevelEnabled(LogLevel.DEBUG);
            if (debugEnabled)
                getLogger().debug("Configuring context for user ({})", config.getLogin());
            config.getGroups().add(PUBLIC_GROUP);
            Node configurators = getNodeOrThrowEx(UserContextConfiguratorsNode.NAME);
            try {
                bindingSupport.put(BindingNames.USER_CONTEXT_CONFIGURATOR_BINDING, config);
                for (UserContextConfigurator ctxCfgr: getEffectiveChildsOfType(configurators, UserContextConfigurator.class))            
                    ctxCfgr.configure(config);
                return new UserContextImpl(config, getGroupsNode());
            } finally {
                bindingSupport.reset();
            }
        } finally {
            configureStat.markOperationProcessingEnd(ts);
        }
    }

    private void informLoginListeners(UserContext userContext) throws Exception {
        long ts = loginListenersStat.markOperationProcessingStart();
        try {
            Node listeners = getNodeOrThrowEx(LoginListenersNode.NAME);
            for (LoginListener listener: getChildsOfType(listeners, LoginListener.class))
                listener.userLoggedIn(userContext);
        } finally {
            loginListenersStat.markOperationProcessingEnd(ts);
        }
    }

}
