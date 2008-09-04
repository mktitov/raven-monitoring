package org.raven.ui;

import java.io.IOException;
import java.io.InputStream;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;

import org.apache.myfaces.custom.dynamicResources.ResourceContext;
import org.apache.myfaces.custom.graphicimagedynamic.ImageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RavenViewableImageRenderer implements ImageRenderer 
{
    protected Logger logger = LoggerFactory.getLogger(RavenViewableImageRenderer.class);	
	public static final String PARAM_NAME = "ViewableObject";
	//private InputStream stream = null;
	private ViewableObjectWrapper vow = null;
	
	public int getContentLength() 
	{
		return -1; 
	}

	public String getContentType() 
	{
		if(vow==null) return "none";
		return vow.getMimeType();
	}

	public void renderResource(ResponseStream out) throws IOException 
	{
		if(vow==null) return;
		InputStream is;
		try {
		is = (InputStream) vow.getData();
		} catch(ClassCastException e)
		{
			logger.error("getData returns not InputStream : ",e);
			return;
		}
		int bufLen = 100000;
		byte[] ba = new byte[bufLen];
		int cnt;
		while( (cnt = is.read(ba))!=-1 )
		{
			if(cnt==0) continue;
			out.write(ba, 0, cnt);
		}
	}

	public void setContext(FacesContext fc, ResourceContext rc) throws Exception 
	{
		String par_value = fc.getExternalContext().getRequestParameterMap().get(PARAM_NAME);
		vow = SessionBean.getInstance().getViewableObjectsStorage().get(par_value);
		if(vow==null) logger.error("ViewableObjectWrapper is null !");

	}

    @SuppressWarnings("unchecked")
	public Class getImageRenderer()
    {
        return this.getClass();
    }
	
}
