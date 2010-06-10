package org.raven.auth.impl;

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
		public static final String SHOW_PARAM = "show";
		/**
		 * Путь до целевого узла
		 */
		private String show = null;
		private boolean present = true;
//		private long lastCheck = 0;

		public AccessResource(String list)
		{
			super.init(list);
		}
		
		protected boolean applyExpression(String[] x) 
		{
			if(x[0].equals(SHOW_PARAM))
			{
				setShow(x[1]);
				return true;
			}
			return false;		
		}

	    public boolean isValid()
	    {
	    	if(getName()==null) return false;
    		if( getAcl().size() > 0 ) return true;
    		if( getShow()!=null && getTitle()!=null) return true;
	    	return false;
	    }

	    /**
		 * Устанавливает путь до целевого узла
	     * @param show
	     */
		public void setShow(String show) {
			this.show = show;
		}
		
		private static final String notPresent = "not present";
		/**
		 * 
		 * @return путь до целевого узла
		 */
		public String getShow() 
		{
			try { if(show==null)
					show = getFirst().getNodePath(); }
			catch(Exception e) {
				logger.warn("on getShow:", e);
				show = notPresent;
			}
			return show;
		}

		public void setPresent(boolean present) {
			this.present = present;
		}

		public boolean isPresent() {
			return present;
		}
		
}
