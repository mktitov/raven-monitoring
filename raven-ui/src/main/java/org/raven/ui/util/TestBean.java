package org.raven.ui.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.apache.myfaces.trinidad.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.myfaces.trinidad.component.core.input.CoreInputFile;

public class TestBean 
{
	private Logger logger = LoggerFactory.getLogger(TestBean.class);	
	private UploadedFile file = null;
	private CoreInputFile ifile;
	
    public synchronized UploadedFile getFile()
    {
    	logger.info("getFile()");
        return file;
    }

    public synchronized void setFile(UploadedFile file)
    {
    	logger.info("setFile({})",file.getFilename());
    	this.file = file;
    }
/*
    public void fileUploaded(ValueChangeEvent event)
    {
    	logger.info("fileUpload");
    	UploadedFile f = (UploadedFile) event.getNewValue();
    	setFile(f);
    }
*/
    public void fileUploaded(ValueChangeEvent event)
    {
    	logger.info("fileUpload");
      UploadedFile file = (UploadedFile) event.getNewValue();
      if (file != null)
      {
        FacesContext context = FacesContext.getCurrentInstance();
        FacesMessage message = new FacesMessage(
           "Uploaded file " + file.getFilename() +
           " (" + file.getLength() + " bytes)");
        context.addMessage(event.getComponent().getClientId(context), message);
      }
    }
    
    public String doUpload()
    {
    	logger.info("doUpload");
    	//UploadedFile file = getFile();
    	//UploadedFile file = (UploadedFile) getIfile().getValue();
    	//logger.info("!!! "+file.getFilename());
      // ... and process it in some way
      return null;
    }

	public void setIfile(CoreInputFile ifile) 
	{
		logger.info("setIfile()");
		this.ifile = ifile;
	}

	public CoreInputFile getIfile() {
		logger.info("getIfile()");
		return ifile;
	}

    
}
