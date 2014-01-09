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

import java.util.List;
import javax.script.Bindings;
import org.raven.BindingNames;
import org.raven.annotations.NodeClass;
import org.raven.annotations.Parameter;
import org.raven.auth.AuthenticationFailedException;
import org.raven.auth.Authenticator;
import org.raven.auth.IllegalLoginException;
import org.raven.auth.IllegalPasswordException;
import org.raven.auth.IpFilter;
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
import org.slf4j.Logger;

/**
 *
 * @author Mikhail Titov
 */
@NodeClass(parentNode=LoginManagerNode.class)
public class LoginServiceNode extends BaseNode implements LoginService {
    
    private OperationStatistic ipFiltersStat;
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
        ipFiltersStat = new OperationStatistic();
        loginStat = new OperationStatistic();
        authStat = new OperationStatistic();
        configureStat = new OperationStatistic();
        loginListenersStat = new OperationStatistic();
    }
    
    protected boolean createAuthenticatorsNode() {
        return true;
    }
    
    protected void initChildren() {
        if (!hasNode(IpFiltersNode.NAME))
            addAndStart(new IpFiltersNode());
        if (!hasNode(AuthenticatorsNode.NAME) && createAuthenticatorsNode())
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
    public OperationStatistic getIpFiltersStat() {
        return ipFiltersStat;
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
    
    public IpFiltersNode getIpFiltersNode() {
        return (IpFiltersNode) getNode(IpFiltersNode.NAME);
    }
    
    private void addAndStart(Node node) {
        addAndSaveChildren(node);
        node.start();
    }

    public boolean isLoginAllowedFromIp(String ip) {
        final long ts = ipFiltersStat.markOperationProcessingStart();
        try {
            return isIpAllowed(getIpFiltersNode(), getLogger(), ip);
        } finally {
            ipFiltersStat.markOperationProcessingEnd(ts);
        }
    }
    
    public static boolean isIpAllowed(Node filtersNode, Logger logger, String ip) {
        List<IpFilter> filters = getChildsOfType(filtersNode, IpFilter.class);
        if (filters.isEmpty())
            return true;
        for (IpFilter filter: getChildsOfType(filtersNode, IpFilter.class))
            try {
                if (filter.isIpAllowed(ip))
                    return true;
            } catch (Exception e) {
                if (logger.isErrorEnabled())
                    logger.error(String.format("Error in (%s) ip filter", ((Node)filter).getName()));
            }
        return false;
    }
    
    public UserContext login(String login, String password, String ip) throws LoginException {
        try {
            if (login==null || login.trim().isEmpty())
                throw new IllegalLoginException();
            if (password==null || password.trim().isEmpty())
                throw new IllegalPasswordException();
            boolean debugEnabled = isLogLevelEnabled(LogLevel.DEBUG);
            long ts = loginStat.markOperationProcessingStart();
            UserContextConfig userConfig = authenticateUser(login, password, ip);
            if (debugEnabled)
                getLogger().debug("User ({}) successfully authenticated by authenticator ({})"
                        , login, userConfig.getAuthenticator());
            UserContext userContext = configureUserContext(userConfig);
            informLoginListeners(userContext);
            if (debugEnabled)
                getLogger().debug("User ({}) successfully logged in", login);
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

    private UserContextConfig authenticateUser(String login, String password, String ip) throws Exception {
        long ts = authStat.markOperationProcessingStart();
        try {
            boolean debugEnabled = isLogLevelEnabled(LogLevel.DEBUG);
            if (debugEnabled)
                getLogger().debug("Authenticating user ({})", login);
            Node authenticators = getNodeOrThrowEx(AuthenticatorsNode.NAME);
            UserContextConfig user = new UserContextConfigImpl(login, ip);
            for (Authenticator auth: getChildsOfType(authenticators, Authenticator.class)) 
                if (auth.checkAuth(user, password)) 
                    return user;
            if (isLogLevelEnabled(LogLevel.WARN))
                getLogger().warn("User ({}) authentication was unsuccessfull", login);
            throw new AuthenticationFailedException(login, getName());
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
            bindingSupport.put(BindingNames.USER_BINDING, userContext);
            for (LoginListener listener: getEffectiveChildsOfType(listeners, LoginListener.class))
                listener.userLoggedIn(userContext);
        } finally {
            bindingSupport.reset();
            loginListenersStat.markOperationProcessingEnd(ts);
        }
    }

}
