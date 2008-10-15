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

package org.raven.ui.attr;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.ui.SessionBean;

public class EditExpressionBean 
{
	public static final String PARAM_NAME = "editedRow";
	private Attr attr = null;
	private String expression = "";
	
	public String getParamName() { return PARAM_NAME; }

	public void selectAttr(ActionEvent event)
	{
		  UIComponent component=event.getComponent();
		  if(component==null)
		  {
			 // logger.info("component==null");
			  return;
		  }	  
		  Map<String, Object> params = component.getAttributes();
		  attr = (Attr) params.get(PARAM_NAME);
		  if(attr!=null) expression = attr.getExpression();
	}

	public void handleReturn(ReturnEvent event)
	{
		SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
		sb.reloadBothFrames();
	}
	  
	public String write()
	{
		attr.setExpression(expression);
		RequestContext.getCurrentInstance().returnFromDialog(expression, null);
		return null;
	}
		
	public String cancel()
	{
		RequestContext.getCurrentInstance().returnFromDialog(null, null);
		return null;
	}
	 
	public Attr getAttr() { return attr; }
	public void setAttr(Attr attr) { this.attr = attr; }

	public String getExpression() { return expression; }
	public void setExpression(String expression) { this.expression = expression; }
 	
}
