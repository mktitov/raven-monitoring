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

import java.io.IOException;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import org.apache.myfaces.custom.dynamicResources.ResourceContext;
import org.apache.myfaces.custom.graphicimagedynamic.ImageRenderer;
import org.apache.tapestry.ioc.Registry;
import org.raven.DynamicImageNode;
import org.raven.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RavenImageRenderer implements ImageRenderer 
{
    protected Logger logger = LoggerFactory.getLogger(RavenImageRenderer.class);
    //private NodeWrapper wrapper;
    private DynamicImageNode din;
    
    public RavenImageRenderer() { }
	
	public int getContentLength() { return -1; }

	public String getContentType() 
	{
		String s = din.getImageFormat().toString().toLowerCase();
		return "image/"+s; 
	}
	
	public int getHeight() { return din.getHeight(); }
	
	public int getWidth() { return din.getWidth(); }

	public void renderResource(ResponseStream out) throws IOException 
	{
		int bufLen = 100000;
		byte[] ba = new byte[bufLen];
		
		java.io.InputStream is = din.render();
		int cnt;
		while( (cnt = is.read(ba))!=-1 )
		{
			if(cnt==0) continue;
			out.write(ba, 0, cnt);
		}
	}

	public void setContext(FacesContext facesContext, ResourceContext resourceContext) throws Exception 
	{
		FacesContext fc = facesContext;
		String path = fc.getExternalContext().getRequestParameterMap().get("nodePath");
		Registry registry = RavenRegistry.getRegistry();
		Tree tree = registry.getService(Tree.class);
		din = (DynamicImageNode) tree.getNode(path); 
/*		
		FacesContext context = FacesContext.getCurrentInstance();
		javax.el.ELContext ec = context.getELContext();
		*/
		//din = (DynamicImageNode) context.getELContext().getELResolver().getValue(ec, null, parName);
		/*		
		RowExplorer re = (RowExplorer) context.getELContext().getELResolver().getValue(ec, null, parName);
		NodeWrapper nw = (NodeWrapper) re.getRow();
		din = (DynamicImageNode) nw.getNode();
*/		
	    if (din == null)
	        throw new IllegalStateException("node '"+path+"' not found");
	}

    @SuppressWarnings("unchecked")
	public Class getImageRenderer()
    {
        return this.getClass();
    }
	
}
