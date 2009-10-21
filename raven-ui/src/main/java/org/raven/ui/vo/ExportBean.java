package org.raven.ui.vo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.raven.ui.SessionBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportBean
{
    private static final Logger logger = LoggerFactory.getLogger(ExportBean.class);	
	public static final String TAB = "\t";
	public static final String COMMA = ",";
	public static final String SEMICOLON = ";";
	
	private VOTableWrapper object = null;
	private VOTableWrapper table = null;
	
	private boolean csvHeader = true;
	private boolean csvCRLF = true;
	private boolean escapeCSV = true;
	
	private Charset charset;

	public ExportBean()
	{
		charset = (Charset) SessionBean.getInstance().getCharsets()[0].getValue();
	}
	
	public void setTable(VOTableWrapper t)
	{
	  table = t;
	  object = null;
	}
	
	public void export(ActionEvent actionEvent) 
	{
		UIComponent uic = actionEvent.getComponent();
		try {
			CoreTable ct = null;
			for(int i=0; i<12 ;i++)
			{
				uic = uic.getParent();
				if (uic instanceof CoreTable) {
					ct = (CoreTable) uic;
					break;
				}
			}
			if(ct!=null)
			{
				//VOTableWrapper lst = (VOTableWrapper) ct.getValue();
				VOTWModel lst = (VOTWModel) ct.getValue();
				VOTableWrapper x = (VOTableWrapper) lst.getWrappedData();
				setTable(x);
			} else logger.warn("export: not found table");
		}
		catch(ClassCastException e)	{ logger.error("export: ",e); }
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
		String fName = getFileName(ext);
		String fn="noname";
		try {
			fn = new String(fName.getBytes(charset),"Cp1252");			
			//fn = javax.mail.internet.MimeUtility.encodeText(fName, charset.name(), "B");
		} catch (UnsupportedEncodingException e1) {
			SessionBean sb = SessionBean.getInstance();
			String u = "account='"+sb.getAccountName()+"' ip='"+sb.getRemoteIp()+"'";
			logger.info("on writeResponce, {} : {}",u,e1.getMessage());
		}
		response.setHeader("Content-disposition", "attachment; filename=" + fn);
		response.setContentType(ct);
		response.setCharacterEncoding(charset.toString());

		//PrintWriter out = null;
		OutputStream os = null;
		try 
		{
			//out = response.getWriter();
		   	//out.print(x);
			byte[] z = x.getBytes(charset);
			os = response.getOutputStream();
			os.write(z);
		   	//out.print(x);
		}
		catch (IOException e) { logger.error("on writeResponce: ",e); }
		finally { try {os.close();} catch(Exception e) {}}
		fc.responseComplete(); 			
	}
	
	public void exportToExcel(ActionEvent actionEvent) 
	{
		export(actionEvent);
		VOTableWrapper vtw = getObject();  
		if(vtw==null) return;
		String contentType = "application/vnd.ms-excel";
		writeResponce(vtw.makeHtmlTable(charset.toString()), contentType, "xls");
    }

	public void exportToHtml(ActionEvent actionEvent) 
	{
		export(actionEvent);
		VOTableWrapper vtw = getObject();  
		if(vtw==null) return;
		String contentType = "text/html";
		writeResponce(vtw.makeHtmlTable(charset.toString()), contentType, "html");
    }
	
	public void exportToCSV(ActionEvent actionEvent,String delim) 
	{
		export(actionEvent);
		VOTableWrapper vtw = getObject();  
		if(vtw==null) return;
		String contentType = "text/csv";
		writeResponce(vtw.makeCSV(csvHeader,csvCRLF,delim,escapeCSV), contentType, "csv");
    }

	public void exportToCSVwithComma(ActionEvent actionEvent) 
	{
		exportToCSV(actionEvent,COMMA);
    }

	public void exportToCSVwithTab(ActionEvent actionEvent) 
	{
		exportToCSV(actionEvent,TAB);
    }

	public void exportToCSVwithSemi(ActionEvent actionEvent) 
	{
		exportToCSV(actionEvent,SEMICOLON);
    }
	
	public void setCsvHeader(boolean csvHeader) {
		this.csvHeader = csvHeader;
	}

	public Boolean getCsvHeader() {
		return csvHeader;
	}

	public boolean isCsvHeader() {
		return csvHeader;
	}
	
	public void setCsvCRLF(boolean csvCRLF) {
		this.csvCRLF = csvCRLF;
	}

	public Boolean getCsvCRLF() {
		return csvCRLF;
	}

	public boolean isCsvCRLF() {
		return csvCRLF;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public Charset getCharset() {
		return charset;
	}

	public void setEscapeCSV(boolean escapeCsv) {
		this.escapeCSV = escapeCsv;
	}

	public boolean isEscapeCSV() {
		return escapeCSV;
	}
	
}
