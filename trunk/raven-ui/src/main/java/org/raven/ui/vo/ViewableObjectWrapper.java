/* 
 *  Copyright 2008 Sergey Pinevskiy.
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
package org.raven.ui.vo;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.event.LaunchEvent;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.table.ColumnGroup;
import org.raven.table.Table;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.AttributeValueValidationException;
import org.raven.tree.InvalidPathException;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.raven.tree.ViewableObject;
import org.raven.tree.Node;
import org.raven.tree.UploadFileViewableObject;
import org.raven.tree.impl.UploadedFileImpl;
import org.raven.ui.SessionBean;
import org.raven.ui.attr.Attr;
import org.raven.ui.node.NodeWrapper;
import org.raven.util.Utl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.weda.beans.ObjectUtils;
import org.weda.constraints.TooManyReferenceValuesException;
import org.weda.internal.annotations.Service;

/**
*
* @author Sergey Pinevskiy
*/
public class ViewableObjectWrapper 
{
    public static final String NAVIGATE_TO = "navigateToNode";
	public static final String NODE_URL = "nodeUrl";
	public static final String RAVEN_TABLE_GR = "ravenTable";
	public static final String RAVEN_TEXT = "ravenText";
    public static final String RAVEN_UPLOAD_FILE = "ravenUploadFile";
	public static final String IMAGE = "image";
	public static final String ACTION = "action";
	public static final String UID_DELIM = "@";

	private static final Logger log = LoggerFactory.getLogger(ViewableObjectWrapper.class);

    @Service
    private static Auditor auditor;

	private ViewableObject viewableObject = null;
	private NodeWrapper nodeWrapper = null;
	private long fd = 0;
//	private String htmlTable = null;
	private byte[] image = null;
	private VOTableWrapper tableWrapper = null;
	private int uid;
	private int nodeId;
//	private List<TableItemWrapper[]> tableData  = null;
//	private String[] tableColumnNames  = null;
//	private boolean[] valid = null;
	private List<Attr> actionAttributes = null;
	private boolean actionRunned = false;
    private boolean hasValidationErrors = false;
	private String actionRet = "";
    private int counter = 0;
    private UploadedFile uploadedFile;
    private UploadedFile file;
	
	public ViewableObjectWrapper(ViewableObject vo)
	{
		if(Viewable.RAVEN_NODE_MIMETYPE.equals(vo.getMimeType()))
		{
			try { 
				Node n  = SessionBean.getTree().getNode((String)vo.getData());
				nodeWrapper = new NodeWrapper(n);
			} 
			catch (InvalidPathException e) 
			{  log.error("Invalid node path '{}' ",vo.getData(),e); 	}
		}
		else viewableObject = vo;	
		setFd();
	}
	
	public ViewableObjectWrapper(Node node)
	{
		setFd();
		this.nodeWrapper = new NodeWrapper(node);
	}

	public String getId()
	{
		return toString();
	}

	public String getIdVO()
	{
		if(viewableObject==null) return "";
		return viewableObject.toString(); 
	}

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
    	log.info("setFile: '{}'", file==null? "null" : file.getName());
        this.file = file;
    }

    public String uploadFile()
    {
        if (isUploadFile() && file!=null) {
            log.info("Uploading file ({})", file.getName());
            try {
                try {
                    UploadFileViewableObject uploader = (UploadFileViewableObject) viewableObject;
                    uploader.setUploadedFile(
                        new UploadedFileImpl(file.getContentType(), file.getName(), file.getInputStream()));
                    AuditRecord rec = auditor.prepare(uploader.getNode(), getAccountName()
                            , Action.ATTR_CH_VALUE, "Uploaded file ({})", file.getName());
                    auditor.write(rec);
                } catch (IOException ex) {
                    log.error("File ({}) upload error", file.getName());
                }
            } finally {
                file = null;
            }
        }
        return null;
    }
    
	public static String getAccountName()
	{
		return SessionBean.getUserContext().getLogin();
	}

	
	public String getHeight() 
	{ 
		if(viewableObject==null) return "";
		return ""+viewableObject.getHeight(); 
	}
	
	public String getWidth() 
	{ 
		if(viewableObject==null) return "";
		return ""+viewableObject.getWidth(); 
	}
	
	public boolean isViewable()
	{
		if(viewableObject==null) return false;
		return true;
	}

	public boolean isNodeUrl()
	{
		if(nodeWrapper==null) return false;
		return true;
	}

	public boolean isAction()
	{
		if(isViewable() && getMimeGroup().equals(ACTION)) return true;
		return false;
	}

    public boolean isUploadFile()
    {
        return isViewable() && viewableObject instanceof UploadFileViewableObject;
    }

    public boolean isRefreshViewAfterAction()
    {
        boolean res = isAction() && ((ActionViewableObject)viewableObject).isRefreshViewAfterAction();
        log.warn("isRefreshViewAfterAction: {}", res);
        return res;
    }
	
	public boolean isActionHasAttributes()
	{
		if (viewableObject instanceof ActionViewableObject) {
			ActionViewableObject a = (ActionViewableObject) viewableObject;
			Collection<NodeAttribute> c = a.getActionAttributes();
			if(c!=null && c.size()>0)
				return true;
		}
		return false;		
	}

	public List<Attr> getActionAttributes()
	{
		actionAttributes = new ArrayList<Attr>();
		if (viewableObject instanceof ActionViewableObject) {
			ActionViewableObject a = (ActionViewableObject) viewableObject;
			Collection<NodeAttribute> c = a.getActionAttributes();
			if(c!=null && c.size()>0)
				for(NodeAttribute na :c)
					try { actionAttributes.add(new Attr(na));
					} catch (TooManyReferenceValuesException e) {
						log.warn("getActionAttributes: {}", e.getMessage());
					}
		}
		return actionAttributes;		
	}
	
	public boolean isShowAttributesDialog()
	{
		if(getConfirmationMessage()!=null && isActionHasAttributes())
			return true;
		return false;
	}
	
	public void setDialogParameter(LaunchEvent event)
	{
		if (viewableObject instanceof ActionViewableObject) {
			//ActionViewableObject a = (ActionViewableObject) viewableObject;
			event.getDialogParameters().put("actionVOW", this);
		}
	  }
	
	public String getConfirmationMessage()
	{
		if (viewableObject instanceof ActionViewableObject) {
			ActionViewableObject a = (ActionViewableObject) viewableObject;
			return a.getConfirmationMessage();
		}
		return null;
	}

	public String getEscapedConfirmationMessage()
	{
		return StringEscapeUtils.escapeHtml(getConfirmationMessage());
	}
	
	public boolean isActionVO()
	{
		if (viewableObject instanceof ActionViewableObject)  
			return true; 
		return false;
	}
	
	public boolean isImage()
	{
		if(isViewable() && getMimeGroup().equals(IMAGE)) return true;
		return false;
	}

	public boolean isText()
	{
		if(isViewable() && viewableObject.getMimeType().equals(Viewable.RAVEN_TEXT_MIMETYPE))
			return true;
		return false;
	}
	
	public boolean isFileUpload()
	{
		if(!isViewable()) return false;
		if(isImage() || isTable()) return false;
		return true;
	}
	
	public boolean isTable()
	{
		if(tableWrapper!=null) return true;
		if(!isViewable() || 
				!viewableObject.getMimeType().equals(Viewable.RAVEN_TABLE_MIMETYPE))
			return false;
		Object x = viewableObject.getData();
		if(x==null) return false;
		if (x instanceof Table) 
		{
			tableWrapper = new VOTableWrapper((Table) x);
			return true;
		}
		return false;
	}

	private void returnFromDialog() 
	{
		actionRunned = false;		
		RequestContext.getCurrentInstance().returnFromDialog(null, null);
	}

    public boolean isHasValidationErrors() {
        return hasValidationErrors;
    }

	public boolean isActionRunned() {
		return actionRunned;
	}
	
	public String getActionRet() {
		return actionRet;
	}
	
	public String cancel() 
	{
		returnFromDialog();
		return null;
	}

	public String close() 
	{
		returnFromDialog();
		if(((ActionViewableObject)viewableObject).isRefreshViewAfterAction())
        {
            log.warn("Refreshing viewable objects");
			SessionBean.getNodeWrapper().onRefresh();
        }

		return null;
	}

    public void handleActionDialogReturn(ReturnEvent event)
    {
        log.error("Handling return from action dialog");
		if ( ((ActionViewableObject)viewableObject).isRefreshViewAfterAction() )
        {
            log.error("Reloading right frame");
            SessionBean sb = (SessionBean) SessionBean.getElValue(SessionBean.BEAN_NAME);
            sb.reloadRightFrame();
        }
    }
	
	public String run()  
	{
		StringBuilder sb = new StringBuilder();
		if(actionAttributes!=null) 
			try {
                hasValidationErrors = false;
				for(Attr a :  actionAttributes)
                {
					String v = a.getValue();
					String n = a.getName();
                    try {
                        a.getAttribute().setValue(v);
                        a.setErrors(null);
                    } catch(AttributeValueValidationException e){
                        hasValidationErrors = true;
                        log.error("Attribute {} has validation errors: {}", n, a.getErrors());
                    }
					sb.append("name='").append(n).append("' , ");
					sb.append("value='").append(v).append("' ; ");
				}	
			} catch (Exception e) {
				log.error("set actionAttributes:", e);
			}
        if (!hasValidationErrors) {
            runAction(sb.toString(), true);
            log.warn("runAction ok");
        } else
            log.warn("Action attributes has validation errors.");
	//	returnFromDialog();
		return null;
	}

	public String runActionD()
	{
		runAction(null, false);
		if ( ((ActionViewableObject)viewableObject).isRefreshViewAfterAction() )
        {
            log.warn("Refreshing viewable objects");
			SessionBean.getNodeWrapper().onRefresh();
        }
        return null;
//		return "dialog:runAction";
	}
	
	private String runAction(String attrInfo, boolean dialog)
	{
		if(!isAction()) return null;
		ActionViewableObject action = (ActionViewableObject) viewableObject;
		String ret;
		SessionBean sb = SessionBean.getInstance();
		StringBuilder mes = new StringBuilder();
		mes.append("<<").append(viewableObject.toString()).append(">>. ");
		
		Action act = Action.ACTION;
		if(attrInfo!=null)
		{
			mes.append(". Attrs: ").append(attrInfo);
			act = Action.ACTION_WITH_ATTR;
		}
		sb.getAuditor().write(sb.getCurrentNode(), sb.getAccountName(), act, mes.toString());
		Object o = action.getData();
        if (o==null && dialog)
            return close();
		ret = o==null? null : o.toString();
		actionRunned = true;
		actionRet = ret;

		return null;
	}
	
/*
	public String getRunAction()
	{
		Object o = viewableObject.getData();
		if(o==null)
			return Messages.getUiMessage(Messages.DONE);
		return o.toString();
	}
*/	
	public String getFromDate()
	{
		return Utl.formatDate(fd);
	}
	
//	public List<TableItemWrapper[]> getTableData()
//	public List<TIWList> getTableData()
	public VOTWModel getTableData()
	{
		if(!isTable())
		{
			log.error("VO isn't table !");
			return null;
		}
		tableWrapper.init();
		return new VOTWModel(tableWrapper);
	}
	
	public String[] getTableColumnNames()
	{
		if(!isTable())
		{
			log.error("VO isn't table !!");
			return null;
		}
		return tableWrapper.getColumnNames();
	}

	public ColumnGroup[] getColumnGroups()
	{
		if(!isTable())
		{
			log.error("VO isn't table !!");
			return null;
		}
		return tableWrapper.getColumnGroups();
	}

    public List getTextNames(){
        log.error("!!! getTextNames");
        return Arrays.asList("col1", "col2");
    }
        

    public int getGroupsCountAndResetCounter()
    {
        counter = 1;
        log.warn("Counter initialized: {}", counter);
        return getColumnGroups().length;
    }

    public int getGroupsCount()
    {
        return getColumnGroups().length;
    }

    public int getCounterAndIncrement()
    {
        log.warn("Counter incremeneted: {}", counter);
        return counter++;
    }

    public int getCounter()
    {
        return counter;
    }

    public boolean isColumnGroup(int col)
    {
		if(!isTable())
		{
			log.error("VO isn't table !!");
			return false;
		}
		return !tableWrapper.getColumnGroups()[col].getColumnNames().isEmpty();
    }

    public List getValidColumns()
    {
        return new ValidColumns();
    }

    public List getColumnNames()
    {
        return new ColumnNames();
    }

/*
    public boolean[] getValid()
    {
		if(!isTable())
		{
			logger.error("valid - VO isn't table");
			return null;
		}
		return tableWrapper.getValid();
    }
*/
    public int getColumnsCount()
    {
		if(!isTable())
		{
			log.error("getColumnsCount - VO isn't table");
			return 0;
		}
		return tableWrapper.getColumnsCount();
    }
    
	public String getMimeGroup()
	{
		if (isNodeUrl()) return NODE_URL;
		if (isTable()) return RAVEN_TABLE_GR;
		if (isText()) return RAVEN_TEXT;
        if (isUploadFile()) return RAVEN_UPLOAD_FILE;
		String mtype = viewableObject.getMimeType();
		String[] sa = mtype.split("/");
		if(sa.length>1 && ACTION.equals(sa[1]))
			return ACTION;
		//if(IMAGE.equals(sa[0])) return IMAGE;
		//return mtype;
		return sa[0];
	}
	
	public String getMimeType()
	{
		if(!isViewable()) return getMimeGroup();
		return viewableObject.getMimeType();
	}
	
	public Object getData()
	{
		//logger.info("get data");
		if(!isViewable()) return nodeWrapper.getPath();
		if(isImage())
		{
			if(image==null)
			{
				Object dt = viewableObject.getData();
				if (dt instanceof InputStream) 
				{
					InputStream is = (InputStream)dt;
					try { image = IOUtils.toByteArray(is);}
					catch(Exception e) { log.error("getData:",e); }
					finally { try {is.close();} catch(Exception e) {}; }
				}
				else image = (byte[]) dt;
			}	
			return image;
		}
		//Object o = viewableObject.getData();
		//logger.warn("VOW:getData(): "+o);
		return viewableObject.getData();
	}
	
	public String getNodePath()
	{
		if(!isViewable()) return nodeWrapper.getPath();
		return null;
	}

	public String getNavigateTo()
	{
		if(!isViewable())
		{
			Node x = NodeWrapper.getNodeByAttr(nodeWrapper.getNode(),NAVIGATE_TO);
			if(x==null) x = nodeWrapper.getNode();
			return x.getPath();
		}	
		return null;
	}
	
	public ViewableObject getViewableObject() {
		return viewableObject;
	}

	public Node getNode() 
	{
		return nodeWrapper.getNode();
	}

	public NodeWrapper getNodeWrapper() 
	{
		return nodeWrapper;
	}
	
	private void setFd() 
	{
		fd = System.currentTimeMillis();
	}
	
	public long getFd() 
	{
		return fd;
	}

	public void setUid(int uid) 
	{
	//	logger.info("setUid():"+uid);
		this.uid = uid;
	}

	public int getUid() 
	{
	//	logger.info("getUid():"+uid);
		return uid;
	}

	public void setNodeId(int nodeId) 
	{
	//	logger.info("setNodeId():"+nodeId);
		this.nodeId = nodeId;
	}

	public int getNodeId() 
	{
	//	logger.info("getNodeId():"+nodeId);
		return nodeId;
	}
	
	public String getComplexUid() {
		return ""+getNodeId()+UID_DELIM+getUid();
	}

//    private static int decodeIndexToColumnNumber(int index)
//    {
//        int grp = index/1000;
//        int col = (int) ((index/1000. - grp) * 1000);
//    }
//
    private class ValidColumns extends AbstractList<Boolean>
    {
        @Override
        public Boolean get(int i) {
            log.error("Index: {}", i);
            return tableWrapper.isValidIndex(i);
        }

        @Override
        public int size() {
            return 10000;
        }
    }

    private class ColumnNames extends AbstractList<String>
    {
        @Override
        public String get(int i) {
            return tableWrapper.getColumnName(i);
        }

        @Override
        public int size() {
            return 1;
        }
    }
}
