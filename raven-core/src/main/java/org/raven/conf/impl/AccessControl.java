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

package org.raven.conf.impl;

public class AccessControl {
	public static final int READ = 1; 
	public static final int WRITE = 2; 
	public static final int NONE = 0; 
	
	private String resource = "";
	private int right = 0;
	
	public AccessControl(String rule)
	{
		String[] x = rule.split(":");
		if(x.length==2)
		{
			resource = x[0];
			String tmp = x[1].toLowerCase();
			if(tmp.length()==0 || tmp.charAt(0)=='n') right = NONE;
				else if(tmp.charAt(0)=='r') right = READ;
					else if(tmp.charAt(0)=='w') right = WRITE;
		}
	}

	public synchronized String getResource() { return resource; }
	public synchronized int getRight() { return right; }

}
