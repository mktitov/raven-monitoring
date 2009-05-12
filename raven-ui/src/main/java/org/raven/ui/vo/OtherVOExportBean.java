package org.raven.ui.vo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.raven.cache.CacheValueContainer;
import org.raven.tree.ViewableObject;
import org.raven.ui.SessionBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtherVOExportBean 
{
    private static final Logger logger = LoggerFactory.getLogger(ExportBean.class);
    public static final String PAR_NODE_ID = "nodeId";
    public static final String PAR_UID = "uid";
    
	public void export(ActionEvent actionEvent) 
	{
		try {
		Map<String,Object> map = actionEvent.getComponent().getAttributes();
		int nodeId = (Integer)map.get(PAR_NODE_ID);
		int uid = (Integer)map.get(PAR_UID);
		
		SessionBean sb = SessionBean.getInstance();
		VObyNode voCache = sb.getViewableObjectsCache();
		CacheValueContainer<List<ViewableObjectWrapper>> cc = voCache.getValueContainer(nodeId);
		if(cc==null)
		{
			//logger.info("");
			return;
		}
		List<ViewableObjectWrapper> lst = cc.getValue();
		ViewableObjectWrapper wrp = null;
		for(ViewableObjectWrapper wr : lst)
			if(wr.getUid()==uid) wrp = wr;
		if(wrp==null || wrp.getViewableObject()==null ) return;
		ViewableObject vo = wrp.getViewableObject();
		String contentType = vo.getMimeType();
		
		writeResponce(vo.getData(), contentType, vo.toString());
		}
		catch(Exception e)
		{
			logger.error("on export VO:", e);
		}
    }

	private void writeResponce(Object data, String ct, String fname)
	{
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
		response.setHeader("Content-disposition", "attachment; filename=" + fname);
		response.setContentType(ct);

		//PrintWriter out = null;
		OutputStream out = null;
		try 
		{
			out = response.getOutputStream(); //.getWriter();
			if (data instanceof byte[]) 
			{
				byte[] ba = (byte[]) data;
				out.write(ba, 0, ba.length); //.print(x);
			}
			if (data instanceof InputStream) 
			{
				int blen = 65536; 
				int len;
				byte[] buf = new byte[blen];
				InputStream is = (InputStream) data;
				while( (len=is.read(buf))!=-1 )
					out.write(buf, 0, len); 
			}
		}
		catch (IOException e) { logger.error("",e); }
		finally { try {out.close();} catch(Exception e) {}}
		fc.responseComplete(); 			
	}
	

}
