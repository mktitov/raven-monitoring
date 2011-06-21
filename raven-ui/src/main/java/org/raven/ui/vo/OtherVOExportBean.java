package org.raven.ui.vo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.io.PrintWriter;
//import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

//import org.apache.tools.ant.input.InputRequest;
import org.apache.commons.io.IOUtils;
import org.raven.cache.CacheValueContainer;
import org.raven.ds.BinaryFieldType;
import org.raven.ds.impl.BinaryFieldValue;
import org.raven.tree.ViewableObject;
import org.raven.ui.SessionBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtherVOExportBean 
{
    private static final Logger logger = LoggerFactory.getLogger(ExportBean.class);
    public static final String PAR_NODE_ID = "nodeId";
    public static final String PAR_UID = "uid";
    public static final String PAR_VO = "vo";
    public static final int BUF_LEN = 65536;
    
    public String getParamVO()
    {
    	return PAR_VO;
    }

	public void export(ActionEvent actionEvent) 
	{
		try 
		{
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
    
    
	public void exportX(ActionEvent actionEvent) 
	{
		try 
		{
			Map<String,Object> map = actionEvent.getComponent().getAttributes();
			ViewableObject vo = (ViewableObject) map.get(PAR_VO);
			String contentType = vo.getMimeType();
			writeResponce(vo.getData(), contentType, vo.toString());
		}
		catch(Exception e)
		{
			logger.error("on export VO from table:", e);
		}
    }

	private void writeResponce(Object data, String ct, String fname)
	{
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
		response.setHeader("Content-disposition", "attachment; filename=" + fname);
		response.setContentType(ct);

		//PrintWriter pout = null;
		OutputStream out = null;
		InputStream is = null;
		try 
		{
			out = response.getOutputStream(); //.getWriter();
			if (data instanceof byte[]) 
			{
				byte[] ba = (byte[]) data;
				out.write(ba, 0, ba.length); //.print(x);
			}
            BinaryFieldType binaryField = null;
            if (data instanceof BinaryFieldType) {
                binaryField = (BinaryFieldType) data;
                data = binaryField.getData();
            }
			if (data instanceof InputStream) 
			{
				int len;
				byte[] buf = new byte[BUF_LEN];
				is = (InputStream) data;
                try {
                    while( (len=is.read(buf))!=-1 )
                        out.write(buf, 0, len); 
                } finally {
                    IOUtils.closeQuietly(is);
                    if (binaryField!=null)
                        binaryField.closeResources();
                }
			}            
		}
		catch (Exception e) { logger.error("",e); }
		finally {
			try {out.close();} catch(Exception e) {}
			if(is!=null) try {is.close();} catch(Exception e) {}
		}
		fc.responseComplete(); 			
	}
}
