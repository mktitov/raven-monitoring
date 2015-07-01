/*
 *  Copyright 2008 Sergey Pinevskiy.
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

package org.raven.ui.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

	public class Messages 
	{
	   //private static final Logger logger = LoggerFactory.getLogger(Messages.class);	
	   public static final String ACCESS_DENIED = "accessDenied";
	   public static final String ACTION = "action";
	   public static final String ACTION_TYPE = "actionType";
	   public static final String ATTR_CANT_DEL = "attributesCantBeDeleted";
	   public static final String ATTR_NOT_FOUND = "attributeNotFound";
	   public static final String ATTRIBUTE = "attribute";
	   public static final String BAD_DST_NODE = "inadmissibleDstNode";
	   public static final String CHARSET1 = "charset1";
	   public static final String CHARSET2 = "charset2";
	   public static final String CHARSET3 = "charset3";
	   public static final String CHARSET4 = "charset4";
	   public static final String DATE = "date";
	   public static final String DONE = "done";
	   public static final String LEVEL = "level";
	   public static final String LOGIN = "login";
	   public static final String NO_SELECTED_NODES = "noSelectedNodes";
	   public static final String NODE_ID = "nodeId";
	   public static final String NODE_PATH = "nodePath";
	   public static final String NODES_CANT_BE_COPIED = "nodesCantBeCopied";
	   public static final String NODES_CANT_BE_MOVED = "nodesCantBeMoved";
	   public static final String NODES_COPY_POSTFIX = "copyPostfix";
	   public static final String NODES_HAVE_DEPEND = "nodesHaveDepend";
	   public static final String MESSAGE = "message";
	   public static final String RA_CHILD = "refreshAttributesOfChildren";
	   public static final String RA_NODE = "refreshAttributesOfNode";
	   public static final String RA_NODE_AND_CHILD = "refreshAttributesOfNodeAndChildren";
       public static final String REMOTE_IP = "remoteIp";
       public final static String PARENT_ATTR_NOT_FOUND = "parentAttributeNotFound";
	   
		public static FacesMessage getMessage(String bundleName, String resourceId, Object[] params) 
	   {
	      FacesContext context = FacesContext.getCurrentInstance();
	      Application app = context.getApplication();
	      String appBundle = app.getMessageBundle();
	      Locale locale = getLocale(context);
	      ClassLoader loader = getClassLoader();
	      String summary = getString(appBundle, bundleName, resourceId, locale, loader, params);
	      if (summary == null) summary = "???" + resourceId + "???";
	      String detail = getString(appBundle, bundleName, resourceId + "_detail", locale, loader, params);
	      return new FacesMessage(summary, detail);
	   }
		
		public static String getUiMessage(String name)
		{
			return Messages.getString("org.raven.ui.messages", name,new Object[] {});
		}

	   public static String getString(String bundle, String resourceId, Object[] params) 
	   {
	      FacesContext context = FacesContext.getCurrentInstance();
	      Application app = context.getApplication();
	      String appBundle = app.getMessageBundle();
	      Locale locale = getLocale(context);
	      ClassLoader loader = getClassLoader();
	      return getString(appBundle, bundle, resourceId, locale, loader, params);
	   }  

	   public static String getString(String bundle1, String bundle2, 
	         String resourceId, Locale locale, ClassLoader loader, Object[] params) 
	   {
	      String resource = null;
	      ResourceBundle bundle;
	      
	      if (bundle1 != null) {
	         bundle = ResourceBundle.getBundle(bundle1, locale, loader);
	         if (bundle != null)
	            try { resource = bundle.getString(resourceId);
	            } catch (MissingResourceException ex) { }
	      }

	      if (resource == null) {
	         bundle = ResourceBundle.getBundle(bundle2, locale, loader);
	         if (bundle != null)
	            try { resource = bundle.getString(resourceId);
	            } catch (MissingResourceException ex) { }
	      }

	      if (resource == null) return null; // no match
	      if (params == null) return resource;
	      
	      MessageFormat formatter = new MessageFormat(resource, locale);      
	      return formatter.format(params);
	   }   

	   public static Locale getLocale(FacesContext context) {
	      Locale locale = null;
	      UIViewRoot viewRoot = context.getViewRoot();
	      if (viewRoot != null) locale = viewRoot.getLocale();
	      if (locale == null) locale = Locale.getDefault();
	      return locale;
	   }
	   
	   public static ClassLoader getClassLoader() {
	      ClassLoader loader = Thread.currentThread().getContextClassLoader();
	      if (loader == null) loader = ClassLoader.getSystemClassLoader();
	      return loader;
	   }
	
}
