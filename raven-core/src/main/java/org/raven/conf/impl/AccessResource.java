package org.raven.conf.impl;
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


public class AccessResource extends AccessControlList 
{
		static final long serialVersionUID = 1;

		public AccessResource(String list)
		{
			super(list);
		}
		
		protected boolean applyExpression(String[] tokens) 
		{
			return false;		
		}

	    public boolean isValid()
	    {
	    	if(getName()!=null && getAcl().size()>0) 
	    		return true;
	    	return false;
	    }
		
		
}
