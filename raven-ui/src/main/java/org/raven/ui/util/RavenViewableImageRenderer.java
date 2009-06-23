package org.raven.ui.util;

import java.io.IOException;
import java.io.InputStream;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import org.apache.myfaces.custom.dynamicResources.ResourceContext;
import org.apache.myfaces.custom.graphicimagedynamic.ImageRenderer;
import org.raven.ui.SessionBean;
import org.raven.ui.vo.ViewableObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RavenViewableImageRenderer implements ImageRenderer 
{
    protected Logger logger = LoggerFactory.getLogger(RavenViewableImageRenderer.class);	
	public static final String PARAM_NAME = "ViewableObject";
	//private InputStream stream = null;
	private ViewableObjectWrapper vow = null;
	public static final int BUF_LEN = 100000;
	
	public int getContentLength() 
	{
		return -1; 
	}

	public int getHeight() 
	{ 
		if(vow==null) return 0;
		return vow.getViewableObject().getHeight(); 
	}
	
	public int getWidth() 
	{ 
		if(vow==null) return 0;
		return vow.getViewableObject().getWidth(); 
	}
	
	public String getContentType() 
	{
		if(vow==null) return "none";
		return vow.getMimeType();
	}

	public void renderResource(ResponseStream out) throws IOException 
	{
		if(vow==null) return;
		Object dt = vow.getData();
		if(dt==null)
		{
			logger.warn("VO data is null");
			return;
		}	
		if (dt instanceof InputStream) 
		{
			int cnt;
			byte[] ba = new byte[BUF_LEN];
			InputStream is = (InputStream)dt;
			try 
			{
				while( (cnt = is.read(ba))!=-1 )
			        if(cnt>0) 
			        	out.write(ba, 0, cnt);
			} catch(Exception e) { logger.error("renderResource:",e); }
			finally	{ try {is.close();} catch(Exception e) {}; }
			return;
		}
		
		byte[] data = (byte[]) dt;
		if(data!=null)
			out.write(data, 0, data.length);
	}

	public void setContext(FacesContext fc, ResourceContext rc) throws Exception 
	{
		String par_value = fc.getExternalContext().getRequestParameterMap().get(PARAM_NAME);
		
		vow = SessionBean.getInstance().getImagesStorage().get(par_value);
		if(vow==null) logger.error("ViewableObjectWrapper is null !");
	}

    @SuppressWarnings("unchecked")
	public Class getImageRenderer()
    {
        return this.getClass();
    }
	
}
