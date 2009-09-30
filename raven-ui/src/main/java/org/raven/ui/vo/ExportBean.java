package org.raven.ui.vo;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportBean
{
    private Logger logger = LoggerFactory.getLogger(ExportBean.class);	
	
	private VOTableWrapper object = null;
	private VOTableWrapper table = null;

	public void setTable(VOTableWrapper t)
	{
	  table = t;
	  object = null;
	}

	public void export(ActionEvent actionEvent) 
	{
		UIComponent uic = actionEvent.getComponent();
		try {
			CoreTable ct = (CoreTable)uic.getParent().getParent().getParent().getParent();
			VOTableWrapper lst = (VOTableWrapper) ct.getValue();
			setTable(lst);
		}
		catch(ClassCastException e)
		{
			logger.error("export: ",e);
		}
    }
	
	
//	public void handleReturn(ReturnEvent event)
//	{
//		SessionBean.getInstance().reloadBothFrames();
//	}

	public String cancel()
	{
		RequestContext.getCurrentInstance().returnFromDialog(null, null);
		return null;
	}
	
	public VOTableWrapper getObject()
	{
		if(table!=null)
		{
			object = table;
			table = null;
		}	
		return object;
	}
	
	private String getFileName(String x)
	{
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
	    return "table-"+fmt.format(new Date())+"."+x;
	}
	
	private void writeResponce(String x, String ct, String ext)
	{
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
		response.setHeader("Content-disposition", "attachment; filename=" + getFileName(ext));
		response.setContentType(ct);

		PrintWriter out = null;
		try 
		{
			out = response.getWriter(); 
		   	out.print(x);
		}
		catch (IOException e) { logger.error("",e); }
		finally { try {out.close();} catch(Exception e) {}}
		fc.responseComplete(); 			
	}
	
	public void exportToExcel(ActionEvent actionEvent) 
	{
		export(actionEvent);
		VOTableWrapper vtw = getObject();  
		if(vtw==null) return;
		String contentType = "application/vnd.ms-excel";
		writeResponce(vtw.makeHtmlTable(), contentType, "xls");
    }

	public void exportToHtml(ActionEvent actionEvent) 
	{
		export(actionEvent);
		VOTableWrapper vtw = getObject();  
		if(vtw==null) return;
		String contentType = "text/html";
		writeResponce(vtw.makeHtmlTable(), contentType, "html");
    }
	
	public void exportToCSV(ActionEvent actionEvent, boolean header) 
	{
		export(actionEvent);
		VOTableWrapper vtw = getObject();  
		if(vtw==null) return;
		String contentType = "text/csv";
		writeResponce(vtw.makeCSV(header), contentType, "csv");
    }

	public void exportToCSVwithoutHeader(ActionEvent actionEvent) 
	{
		exportToCSV(actionEvent,false);
    }
	
	public void exportToCSVwithHeader(ActionEvent actionEvent) 
	{
		exportToCSV(actionEvent,true);
    }

	
}
