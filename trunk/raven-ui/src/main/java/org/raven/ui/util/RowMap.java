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

import java.util.HashMap;


public class RowMap extends HashMap<String,RavenImageRenderer> 
{
	static final long serialVersionUID = 1;
	static public final String BEAN_NAME = "rowMap";
	private RavenImageRenderer r;
	
	public RavenImageRenderer put(String s,RavenImageRenderer rr)
	{
		if(rr!=null) r = rr;
		
		return super.put(s, rr);
	}

	public RavenImageRenderer getRavenImageRenderer()
	{
		return r;
	}
	
	public void setRavenImageRenderer(RavenImageRenderer x)
	{
		 r =x;
	}

}
