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

package org.raven.ui.node;

public class NodeType 
{
	private String className;
	private String shortName;
	private String description;
	
	public NodeType(String className,String shortName, String description)
	{
		this.className = className;
		this.description = description;
		this.shortName =  shortName;
	}
	
	public String getClassName() { return className; }
	public void setClassName(String className) { this.className = className; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }

	public String getShortName() { return shortName; }
	public void setShortName(String shortName) { this.shortName = shortName; }

}
