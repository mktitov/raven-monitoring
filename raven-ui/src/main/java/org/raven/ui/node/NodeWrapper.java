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
package org.raven.ui.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.event.ActionEvent;
import org.apache.myfaces.trinidad.component.core.layout.CoreShowDetailItem;
import org.apache.myfaces.trinidad.event.PollEvent;
import org.apache.myfaces.trinidad.event.ReturnEvent;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.raven.audit.Action;
import org.raven.audit.AuditRecord;
import org.raven.audit.Auditor;
import org.raven.auth.impl.AccessControl;
import org.raven.expr.impl.ExpressionAttributeValueHandlerFactory;
import org.raven.expr.impl.ScriptAttributeValueHandlerFactory;
import org.raven.tree.DataFile;
import org.raven.tree.DataStream;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.NodeError;
import org.raven.tree.ScanOperation;
import org.raven.tree.ScannedNodeHandler;
import org.raven.tree.Viewable;
import org.raven.tree.Node.Status;
import org.raven.tree.impl.BaseNode;
import org.raven.tree.impl.NodeAttributeImpl;
import org.raven.tree.impl.NodeReferenceValueHandler;
import org.raven.tree.impl.NodeReferenceValueHandlerFactory;
import org.raven.tree.impl.ScanOptionsImpl;
import org.raven.ui.IconResource;
import org.raven.ui.ResourcesCache;
import org.raven.ui.SessionBean;
import org.raven.ui.attr.Attr;
import org.raven.ui.attr.NewAttribute;
import org.raven.ui.attr.RefreshAttributesCache;
import org.raven.ui.attr.RefreshIntervalCache;
import org.raven.ui.log.LogView;
import org.raven.ui.util.Messages;
import org.raven.ui.vo.ViewableObjectWrapper;
import org.raven.util.Utl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.weda.beans.ObjectUtils;
import org.weda.constraints.TooManyReferenceValuesException;
//import org.weda.constraints.ConstraintException;
//import org.weda.converter.TypeConverterException;
import org.weda.internal.annotations.Service;

public class NodeWrapper extends AbstractNodeWrapper
        implements Comparator<NodeAttribute>, INodeScanner, ScannedNodeHandler {

    protected static final Logger log = LoggerFactory.getLogger(NodeWrapper.class);
    public static final String BEAN_NAME = "cNode";
    public static final String VO_SOURCE = "viewableObjectSource";
    public static final String VO_HIDE_NODE_NAME = "hideNodeName";
    public static final int MAX_VO_SEARCH = 20;
    public static final String FROM_TO = "'{}' >> '{}'";
    public static final String FROMX_TO = "{} : '{}' >> '{}'";
    public static final String TO = ">> '{}'";
    public static final String ATTEMPT = "attempt:: ";
    @Service
    private Auditor auditor;
    private static final Integer attrRename = 1;
    private static final Integer attrChValue = 2;
    private static final Integer attrChDsc = 3;
    private static final Integer attrChSubType = 4;
    private static final Integer attrChTemplExprFlag = 5;
//	public static final String ATTR = "attribute";
    public static final String strAttribute = Messages.getUiMessage(Messages.ATTRIBUTE);
    private List<NodeAttribute> savedAttrs = null;
    private List<Attr> editingAttrs = null;
    //private List<NodeAttribute> savedRefreshAttrs = null;
    private Map<String, Attr> editingRefreshAttrs = null;
    private List<NodeAttribute> readOnlyAttributes = null;
    private NewAttribute newAttribute = null;
    private CoreShowDetailItem showTab;
    private CoreShowDetailItem nodeEditTab;
    private CoreShowDetailItem treeEditTab;
    private CoreShowDetailItem selNodeTab;
    private CoreShowDetailItem selTemplateTab;
    private boolean useChildAttributesView = true;
//	private boolean hasUnsavedChanges = false;
    private boolean needRefreshVO = false;
    //private int refreshViewInteval = 0;
    private boolean refreshPressed = false;
    private HashMap<Integer, HashMap<Integer, AuditRecord>> unsavedChanges = new HashMap<Integer, HashMap<Integer, AuditRecord>>();
//	private List<AuditRecord> unsavedAuditRecords = new ArrayList<AuditRecord>();
    private Node voSource = null;
    private boolean voSourceInited = false;
    private List<NodeWrapper> upperNodes;
    private int shortNameLen = 0;
    private String iconPath = null;
    private Boolean hideNodeName;
    private Map<String, NodeAttribute> whoulRefreshAttributes = null;
    private LogView logView = null;
    //private LogViewAttributesCache lvaCache = null;

    public NodeWrapper() {
        super();
        SessionBean.initNodeWrapper(this);
    }

    public boolean isRootNode() {
        if (getNode().getParent() == null) {
            return true;
        }
        return false;
    }

    public NodeWrapper(Node node) {
        this();
        this.setNode(node);
    }

    public List<Node> getAccessibleChildrenList(Node n, int threshold) {
        List<Node> ret = new ArrayList<Node>();
        for (Node cn : n.getChildrenList()) {
            if (getUserAcl().getAccessForNode(cn) > threshold) {
                ret.add(cn);
            }
        }
        return ret;
    }

    public String getShortName() {
        String name = getNodeName();
        if (name != null && shortNameLen > 0) {
            if (name.length() > shortNameLen) {
                name = name.substring(0, shortNameLen - 3) + "...";
            }
        }
        return name;
    }

    public List<NodeWrapper> getUpperNodes() {
        if (upperNodes != null) {
            return upperNodes;
        }
        upperNodes = new ArrayList<NodeWrapper>();
        List<Node> rt = new ArrayList<Node>();
        Node n = getNode();
        while (n != null) {
            rt.add(n);
            n = n.getParent();
        }

        int sumNamesLen = 0;
        for (int i = rt.size() - 1; i >= 0; i--) {
            n = rt.get(i);
            if (getUserAcl().getAccessForNode(n) == AccessControl.TRANSIT) {
                int cnt = 0;
                for (Node cn : n.getChildrenList()) {
                    if (getUserAcl().getAccessForNode(cn) > AccessControl.NONE) {
                        cnt++;
                    }
                }
                if (cnt < 2) {
                    continue;
                }

            }
            NodeWrapper nw = new NodeWrapper(n);
            upperNodes.add(nw);
            sumNamesLen += nw.getNodeName().length() + 6;
        }
        int maxLen = 200;
        if (sumNamesLen > maxLen) {
            double k = ((double) sumNamesLen) / maxLen;
            double alen = ((double) sumNamesLen) / upperNodes.size();
            int nlen = (int) (alen / k);
            if (nlen < 7) {
                nlen = 7;
            }
            for (NodeWrapper nw : upperNodes) {
                nw.setShortNameLen(nlen);
            }
        }
        return upperNodes;
    }

    public static boolean getBoolAttrValue(Node cnode, String attr, boolean defValue) {
        NodeAttribute na = cnode.getNodeAttribute(attr);
        if (na == null) {
            return defValue;
        }
        Object n = na.getRealValue();
        if (n == null) {
            return defValue;
        }
        if (n instanceof Boolean) {
            return (Boolean) n;
        } else {
            log.warn("attribute {} of node {} isn't instance of Boolean ", attr, cnode.getPath());
        }
        return defValue;
    }

    public static Node getNodeByAttr(Node cnode, String attr) {
        log.info("curNode {}", cnode.getPath());
        NodeAttribute na = cnode.getNodeAttribute(attr);
        log.info("curNode({})={}", attr, na);
        if (na == null) {
            return null;
        }
        Object n = na.getRealValue();
        log.info("RealValue=" + n);
        if (n == null) {
            return null;
        }
        Node ret = null;
        if (n instanceof Node) {
            ret = (Node) n;
        } else {
            log.warn("attribute {} of node {} is't instance of Node ", attr, cnode.getPath());
        }
        return ret;
    }

    public boolean isHideNodeName() {
        if (hideNodeName == null) {
            return getBoolAttrValue(this.getNode(), VO_HIDE_NODE_NAME, false);
        }
        return hideNodeName;
    }

    public void setHideNodeName(boolean hide) {
        hideNodeName = hide;
    }

    public String getRefreshAttributesTitle() {
        String mesName = null;
        if (isNodeAndChildViewable()) {
            mesName = Messages.RA_NODE_AND_CHILD;
        } else if (isNodeViewableOnly()) {
            mesName = Messages.RA_NODE;
        } else if (isChildViewableOnly()) {
            mesName = Messages.RA_CHILD;
        } else {
            return "";
        }
        return Messages.getUiMessage(mesName);
    }

    public boolean isNeedShowRefreshAttributes() {
        if (!isAllowNodeRead()) {
            return false;
        }
        return isViewable() || isChildViewable();
    }

    public boolean isNodeAndChildViewable() {
        if (!isAllowNodeRead()) {
            return false;
        }
        return isViewable() && isChildViewable();
    }

    public boolean isNodeViewableOnly() {
        return isViewable() && !isChildViewable();
    }

    public boolean isChildViewableOnly() {
        return !isViewable() && isChildViewable();
    }

    public boolean isChildViewable() {
        if (!isAllowNodeRead()) {
            return false;
        }
//		Collection<Node> c = getNode().getEffectiveChildrens();
        Collection<Node> c = getNode().getSortedChildrens();
        if (c != null) {
            Iterator<Node> it = c.iterator();
            while (it.hasNext()) {
                NodeWrapper nw = new NodeWrapper(it.next());
                if (!nw.isAllowNodeRead()) {
                    continue;
                }
                if (nw.isViewable()) {
                    return true;
                }
                //Map<String,NodeAttribute> lst = nw.getRefreshAttributesMap();
            }
        }
        return false;
    }

    public List<NodeWrapper> getViewableChilddren() {
        List<NodeWrapper> ret = new ArrayList<NodeWrapper>();
        Collection<Node> c = getNode().getSortedChildrens();
//		Collection<Node> c = getNode().getEffectiveChildrens();
        if (c != null) {
            Iterator<Node> it = c.iterator();
            while (it.hasNext()) {
                NodeWrapper nw = new NodeWrapper(it.next());
                if (!nw.isAllowNodeRead()) {
                    continue;
                }
                if (nw.isViewable()) {
                    ret.add(nw);
                }
            }
        }
        return ret;
    }

    public int getNodeId() {
        return getNode().getId();
    }

    public boolean isViewable() {
        if (!isAllowNodeRead()) {
            return false;
        }
        if (getVoSource() instanceof Viewable) {
            return true;
        }
        return false;
    }

    public void setNameInDialog(ActionEvent event) {
        RenameNodeBean b = RenameNodeBean.getInstance();
        b.setName(getNode().getName());
    }

    public static String getAccountName() {
        return SessionBean.getUserContext().getLogin();
    }

    public static String mesFormat(String arg0, String arg1) {
        return MessageFormatter.format(arg0, arg1);
    }

    public void renameNodeHandleReturn(ReturnEvent event) {
        if (!isAllowNodeRename()) {
            return;
        }
        String ret = (String) event.getReturnValue();
        if (ret == null) {
            return;
        }
        ret = ret.trim();
        if (ret.length() > 0) {
            Node n = getNode();
            auditor.write(n, getAccountName(), Action.NODE_RENAME, TO, ret);
            n.setName(ret);
            n.save();
            SessionBean.getInstance().reloadRightFrame();
        }
    }

    public void onSetNode() {
        editingAttrs = null;
        createNewAttribute();
        editingRefreshAttrs = null;
        refreshPressed = false;
        clearAllUnsavedChanges();
        setVoSource(null);
        upperNodes = null;
        iconPath = null;
        //if( !SessionBean.isSuperUserS() ) return;
        logView = new LogView(SessionBean.getLvaNode(), getNodeId(), this);

    }

    public void createNewAttribute() {
        if (getClassDesc() != null && getTree() != null) {
            newAttribute = new NewAttribute(getTree().getNodeAttributesTypes(getNode()), getClassDesc());
        }
    }

    public String nodeStart() {
        //if( ! isAllowControl() ) return "err";
        if (!isCanNodeStart()) {
            return "err";
        }
        try {
            getNode().start();
            auditor.write(getNode(), getAccountName(), Action.NODE_START, null);
        } catch (NodeError e) {
            log.error("on start {} : {}", getNodeName(), e.getMessage());
        }
        return "ok";
    }

    public String nodeStartRecursive() {
        //if( ! isAllowControl() ) return "err";
        if (!isNodeAccessible()) {
            return "err";
        }
        try {
            getTree().start(getNode(), true);
            auditor.write(getNode(), getAccountName(), Action.NODE_START_RECURSIVE, null);
        } catch (NodeError e) {
            log.error("on start recursive {} : {}", getNodeName(), e.getMessage());
        }
        return "ok";
    }

    public String nodeStop() {
        //if( ! isAllowControl() ) return "err";
        if (!isCanNodeStop()) {
            return "err";
        }
        try {
            getNode().stop();
            auditor.write(getNode(), getAccountName(), Action.NODE_STOP, null);
        } catch (NodeError e) {
            log.error("on stop {} : {}", getNodeName(), e.getMessage());
        }
        return "ok";
    }

    public String nodeStopRecursive() {
        //if( ! isAllowControl() ) return "err";
        if (!isNodeAccessible()) {
            return "err";
        }
        try {
            getTree().stop(getNode());
            auditor.write(getNode(), getAccountName(), Action.NODE_STOP_RECURSIVE, null);
        } catch (NodeError e) {
            log.error("on start recursive {} : {}", getNodeName(), e.getMessage());
        }
        return "ok";
    }

    public List<Attr> getAttributes() {
        if (editingAttrs == null) {
            loadAttributes();
        }
        if (!useChildAttributesView) {
            return editingAttrs;
        }
        List<Attr> parentAttrs = new ArrayList<Attr>();
        for (Attr a : editingAttrs) {
            if (a.getAttribute().getParentAttribute() == null) {
                parentAttrs.add(a);
            }
        }
        return parentAttrs;
    }

    public Map<String, NodeAttribute> getRefreshAttributesMap() {
        SessionBean sb = SessionBean.getInstance();
        RefreshAttributesCache rs = sb.getRefreshAttributesCache();
        return rs.get(this);
    }

    public List<NodeAttribute> getRefreshAttributes() {
        List<NodeAttribute> nal = new ArrayList<NodeAttribute>();
        Map<String, NodeAttribute> map = getRefreshAttributesMap();
        if (map != null) {
            nal.addAll(map.values());
        }
        return nal;
    }

    public List<ViewableObjectWrapper> getViewableObjects() {
        if (!isAnyAccess()) {
            return Collections.EMPTY_LIST;
        }
        if (editingRefreshAttrs == null) {
            loadRefreshAttributes();
        }
        List<ViewableObjectWrapper> x = SessionBean.getInstance().getViewableObjectsCache().getObjects(this);
        log.info("+++ ");
        for (ViewableObjectWrapper v : x) {
            log.info(" id={}  group={}  nodeId={} uid={}", new Object[]{v.getId(), v.getMimeGroup(), v.getNodeId(), v.getUid()});
        }
        log.info("--- ");
        return x;
    }

    public void removeVOFromHash() {
        SessionBean.getInstance().getViewableObjectsCache().remove(this);
    }

    public void removeRAFromHash() {
        SessionBean.getInstance().getRefreshAttributesCache().remove(this);
    }

    public List<NodeWrapper> getViewableNodes() {
        Collection<Node> c = getNode().getSortedChildrens();
//	  Collection<Node> c = getNode().getEffectiveChildrens();
        ArrayList<NodeWrapper> wrappers = new ArrayList<NodeWrapper>();
        if (c != null) {
            ArrayList<Node> nodes = new ArrayList<Node>();
            nodes.addAll(c);
            for (Node n : nodes) {
                NodeWrapper nw = new NodeWrapper(n);
                if (!nw.isAllowNodeRead()) {
                    continue;
                }
                wrappers.add(nw);
            }
        }
        if (isViewable()) {
            wrappers.add(0, this);
        }
        return wrappers;
    }

    public void loadRefreshAttributes() {
        List<NodeWrapper> wrappers = getViewableNodes();
        if (wrappers.size() > 1) {
            NodeWrapper tmp = wrappers.remove(0);
            wrappers.add(tmp);
        }
        if (editingRefreshAttrs != null) {
            editingRefreshAttrs.clear();
        }
        if (whoulRefreshAttributes != null) {
            whoulRefreshAttributes.clear();
        }
        editingRefreshAttrs = new HashMap<String, Attr>();
        Map<String, NodeAttribute> whoulRA = new HashMap<String, NodeAttribute>();
        for (NodeWrapper nw : wrappers) {
            List<NodeAttribute> nal = nw.getRefreshAttributes();
            for (NodeAttribute na : nal) {
                try {
                    editingRefreshAttrs.put(na.getName(), new Attr(na, true));
                    whoulRA.put(na.getName(), na);
                } catch (TooManyReferenceValuesException e) {
                    log.error("on load refresh attributes : ", e);
                }
            }
        }
        whoulRefreshAttributes = whoulRA;
    }

    public void loadAttributes() {
        loadRefreshAttributes();
        List<NodeAttribute> attrList = getNodeAttributes();
        Collections.sort(attrList, this);
        //if(editingAttrs!=null) editingAttrs.clear();
        //if(readOnlyAttributes!=null) readOnlyAttributes.clear();
        //if(savedAttrs!=null) savedAttrs.clear();

        editingAttrs = new ArrayList<Attr>();
        readOnlyAttributes = new ArrayList<NodeAttribute>();
        savedAttrs = new ArrayList<NodeAttribute>();
        for (NodeAttribute na : attrList) {
            if (na.isReadonly()) {
                readOnlyAttributes.add(na);
            } else {
                if (!SessionBean.isSuperUserS()) {
                    String vht = na.getValueHandlerType();
                    if (ObjectUtils.in(
                            vht,
                            ScriptAttributeValueHandlerFactory.TYPE,
                            ExpressionAttributeValueHandlerFactory.TYPE)) {
                        continue;
                    }
                }
                savedAttrs.add(na);
                try {
                    editingAttrs.add(new Attr(na));
                } catch (TooManyReferenceValuesException e) {
                    log.error("on load attributes: ", e);
                    return;
                }
            }
        }
        for (Attr a : editingAttrs) {
            a.findChildren(editingAttrs);
        }
        //logger.info("loadAttributes()");
    }

//	  public String delAttr()  {   return "";  }
    public String saveWithoutWrite() {
        return save(false);
    }

    public String save(boolean write) {
        int save = 0;
        StringBuilder ret = new StringBuilder();
        if (!isAllowNodeEdit()) {
            return "err";
        }
        Iterator<NodeAttribute> itn = savedAttrs.iterator();
        Iterator<Attr> ita = editingAttrs.iterator();
        NodeAttribute na = null;
        HashMap<Integer, AuditRecord> aRec = new HashMap<Integer, AuditRecord>();
        while (itn.hasNext()) {
            if (!ita.hasNext()) {
                break;
            }
            save = 0;
            na = itn.next();
            Attr at = ita.next();
            if (na.getId() != at.getId()) {
                continue;
            }
            String val = getNotNull(na.getValue());
            String dsc = getNotNull(na.getDescription());
//			  if( !at.isExpressionSupported() &&  !val.equals(at.getValue()) )
            if (!at.isExpressionSupported() && at.isValueChanged()) {
                save |= 1;
                log.info("old value: '{}', new value: '{}'", val, at.getValue());
            }
            if (!dsc.equals(at.getDescription())) {
                save |= 2;
            }
            if (!na.getName().equals(at.getName())) {
                save |= 4;
            }
//			  if( at.isExpressionSupported() &&  ! ObjectUtils.equals(na.getRawValue(), at.getExpression()) ) save |=8;
            if (at.isExpressionSupported() && at.isExpressionChanged()) {
                save |= 8;
            }
            if (!ObjectUtils.equals(na.getValueHandlerType(), at.getValueHandlerType())) {
                save |= 16;
            }
            if (at.isTemplateExpression() != na.isTemplateExpression()) {
                save |= 32;
            }
            if (at.isFileAttribute() && at.getFile() != null) {
                save |= 64;
            }
            if (hasUnsavedChanges(na.getId())) {
                save |= 4096;
                aRec = unsavedChanges.get(na.getId());
            }

            if (save == 0) {
                continue;
            }
            try {
                boolean needNodeCheck = false;
                boolean prevExprInit = false;
                boolean prevVHTInit = false;
                String prevExpr = null;
                String prevVHT = null;
                AuditRecord a;
                if ((save & 1) != 0) {
                    aRec.put(attrChValue, auditor.prepare(getNode(), getAccountName(),
                            Action.ATTR_CH_VALUE, FROMX_TO, na.getName(),
                            getNotNull(na.getValue()), getNotNull(at.getValue())));
                    na.setValue(at.getValue());
                }
                if ((save & 2) != 0) {
                    aRec.put(attrChDsc, auditor.prepare(getNode(), getAccountName(),
                            Action.ATTR_CH_DSC, FROMX_TO, na.getName(),
                            getNotNull(na.getDescription()), getNotNull(at.getDescription())));
                    na.setDescription(at.getDescription());
                }
                if ((save & 4) != 0) {
                    aRec.put(attrRename, auditor.prepare(getNode(), getAccountName(),
                            Action.ATTR_RENAME, FROM_TO, na.getName(), at.getName()));
                    na.setName(at.getName());
                }
                if ((save & 8) != 0) {
                    prevExprInit = true;
                    prevExpr = na.getRawValue();
                    needNodeCheck = true;
                    a = auditor.prepare(getNode(), getAccountName(),
                            Action.ATTR_CH_VALUE, FROMX_TO, na.getName(),
                            getNotNull(prevExpr), getNotNull(at.getExpression()));
                    aRec.put(attrChValue, a);
                    try {
                        na.setValue(at.getExpression());
                    } catch (Exception e) {
                        na.setValue(prevExpr);
                        aRec.remove(attrChValue);
                        a.setMessage(ATTEMPT + a.getMessage());
                        throw e;
                    }

                }
                if ((save & 16) != 0) {
                    prevVHTInit = true;
                    prevVHT = na.getValueHandlerType();
                    needNodeCheck = true;
                    a = auditor.prepare(getNode(), getAccountName(), Action.ATTR_CH_SUBTYPE,
                            FROMX_TO, na.getName(), getNotNull(prevVHT),
                            getNotNull(at.getValueHandlerType()));
                    aRec.put(attrChSubType, a);
                    try {
                        na.setValueHandlerType(at.getValueHandlerType());
                    } catch (Exception e) {
                        na.setValueHandlerType(prevVHT);
                        a.setMessage(ATTEMPT + a.getMessage());
                        throw e;
                    }
                }
                if ((save & 32) != 0) {
                    na.setTemplateExpression(at.isTemplateExpression());
                    aRec.put(attrChTemplExprFlag, auditor.prepare(getNode(), getAccountName(),
                            Action.ATTR_CH_VALUE, FROMX_TO, na.getName() + "<TemplateExpression>",
                            "" + na.isTemplateExpression(), "" + at.isTemplateExpression()));
                }
                if ((save & 64) != 0) {
                    UploadedFile file = at.getFile();
                    if (DataStream.class.isAssignableFrom(at.getAttribute().getType())) {
                        DataStream dataStream = na.getRealValue();
                        dataStream.setStream(file.getInputStream());
                    } else {
                        DataFile dataFile = na.getRealValue();
                        dataFile.setFilename(file.getName());
                        dataFile.setMimeType(file.getContentType());
                        dataFile.setDataStream(file.getInputStream());
                    }
                    log.info("Uploaded: '{}'; size:{} ", file.getName(), file.getSize());
                    a = auditor.prepare(getNode(), getAccountName(), Action.ATTR_CH_VALUE,
                            FROMX_TO, na.getName() + "<File>", ".", file.getName());
                    aRec.put(attrChValue, a);
                    //file.dispose();
                }
                if (needNodeCheck) {
                    if (na.getValueHandler() instanceof NodeReferenceValueHandler) {
                        NodeReferenceValueHandler navh = (NodeReferenceValueHandler) na.getValueHandler();
                        Node x = navh.getReferencedNode();
                        if (x != null) {
                            NodeWrapper nw = new NodeWrapper(x);
                            if (!nw.isAllowNodeEdit()) {
                                if (prevExprInit) {
                                    na.setValue(prevExpr);
                                }
                                if (prevVHTInit) {
                                    na.setValueHandlerType(prevVHT);
                                }
                                if (ret.length() != 0) {
                                    ret.append(". ");
                                }
                                ret.append(strAttribute + " '" + at.getName() + "' : " + Messages.getUiMessage(Messages.ACCESS_DENIED));
                                if (prevExprInit) {
                                    a = aRec.get(attrChValue);
                                    a.setMessage(ATTEMPT + a.getMessage());
                                }
                                if (prevVHTInit) {
                                    a = aRec.get(attrChSubType);
                                    a.setMessage(ATTEMPT + a.getMessage());
                                }
                                continue;
                            }
                        }
                    }
                }
                if (write) {
                    getTree().saveNodeAttribute(na);
                    clearUnsavedChanges(na.getId());
                    if (aRec != null) {
                        for (AuditRecord ar : aRec.values()) {
                            auditor.write(ar);
                        }
                    }
                    this.afterWriteAttrubutes();
                } else {
                    addUnsavedChanges(na.getId(), aRec);
                }
            } /*			  
             catch(ConstraintException e) 
             {
             if(ret.length()!=0) ret.append(". ");
             ret.append(strAttribute+" '"+at.getName()+"' : "+e.getMessage());
             logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
             }
             catch(TypeConverterException e) 
             {
             if(ret.length()!=0) ret.append(". ");
             ret.append(strAttribute+" '"+at.getName()+"' : "+e.getMessage());
             logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
             }
             */ catch (Throwable e) {
                if (ret.length() != 0) {
                    ret.append(". ");
                }
                ret.append(strAttribute + " '" + at.getName() + "' : " + e.getMessage());
                log.info("on set value=" + at.getValue() + " to attribute=" + na.getName(), e);
            } finally {
                at.setValue(na.getValue());
                at.setDescription(na.getDescription());
                at.setName(na.getName());
            }
        }
        //if(ret.length()!=0) ret.toString();
        loadAttributes();
        if (ret.length() != 0) {
            return ret.toString();
        }
        return null;
    }

    public String saveRefreshAttributes() {
        int save = 0;
        StringBuffer ret = new StringBuffer();

        Iterator<String> keys = editingRefreshAttrs.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Attr at = editingRefreshAttrs.get(key);
            List<NodeWrapper> nwlist = getViewableNodes();
            for (NodeWrapper nw : nwlist) {
                Map<String, NodeAttribute> map = nw.getRefreshAttributesMap();
                if (map == null) {
                    continue;
                }
                if (map.containsKey(key)) {
                    NodeAttribute na = map.get(key);
                    String val = getNotNull(na.getValue());
                    if (!at.isExpressionSupported() && !val.equals(at.getValue())) {
                        save |= 1;
                    }
                    if (at.isExpressionSupported() && !ObjectUtils.equals(na.getRawValue(), at.getExpression())) {
                        save |= 8;
                    }
                    if (!ObjectUtils.equals(na.getValueHandlerType(), at.getValueHandlerType())) {
                        save |= 16;
                    }
                    if (at.isTemplateExpression() != na.isTemplateExpression()) {
                        save |= 32;
                    }

                    if (save == 0) {
                        continue;
                    }

                    try {
                        if ((save & 1) != 0) {
                            na.setValue(at.getValue());
                        }
                        if ((save & 8) != 0) {
                            na.setValue(at.getExpression());
                        }
                        if ((save & 16) != 0) {
                            na.setValueHandlerType(at.getValueHandlerType());
                        }
                        if ((save & 32) != 0) {
                            na.setTemplateExpression(at.isTemplateExpression());
                        }
                    } /*					  
                     catch(ConstraintException e) 
                     {
                     if(ret.length()!=0) ret.append(". ");
                     ret.append(strAttribute+" '"+at.getName()+"' : "+e.getMessage());
                     logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
                     }
                     catch(TypeConverterException e) 
                     {
                     if(ret.length()!=0) ret.append(". ");
                     ret.append(strAttribute+" '"+at.getName()+"' : "+e.getMessage());
                     logger.info("on set value="+at.getValue()+" to attribute="+na.getName(), e);
                     }
                     */ catch (Throwable e) {
                        if (ret.length() != 0) {
                            ret.append(". ");
                        }
                        ret.append(strAttribute + " '" + at.getName() + "' : " + e.getMessage());
                        log.info("on set value=" + at.getValue() + " to attribute=" + na.getName(), e);
                    } finally {
                        at.setValue(na.getValue());
                        at.setDescription(na.getDescription());
                        at.setName(na.getName());
                        at.setValueHandlerType(na.getValueHandlerType());
                        at.setTemplateExpression(na.isTemplateExpression());
                    }
                }
            }

        }
        if (ret.length() != 0) {
            ret.toString();
        }
        loadRefreshAttributes();
        return null;
    }

    public String cancel() //ActionEvent event
    {
        getAttributes();
        clearAllUnsavedChanges();
        return "";
    }

    public String cancelRefreshAttributes() //ActionEvent event
    {
        loadRefreshAttributes();
        return "";
    }

    public String createAttribute() throws Exception {
        if (!isAllowNodeEdit()) 
            return "err";
        /*	
         if(newNodeType==null || newNodeType.length()==0)
         {
         logger.warn("no newNodeType");
         return "err";
         }
         */
        if (newAttribute.getName() == null || newAttribute.getName().length() == 0) {
            log.warn("no newAttributeName");
            return "err";
        }
        NodeAttribute na = null;
        na = new NodeAttributeImpl(newAttribute.getName(), newAttribute.getAttrClass(), null, 
                newAttribute.getDescription());
        if (Node.class.isAssignableFrom(na.getType()))
            na.setValueHandlerType(NodeReferenceValueHandlerFactory.TYPE);
        if (!newAttribute.getParentName().isEmpty())
            if (getNode().getAttr(newAttribute.getParentName())==null) {
                getNode().getLogger().error("Error creating attribute {}. "
                        + "Not found parent attribute with name ({})", newAttribute.getName(), newAttribute.getParentName());
                newAttribute.getMessage().setMessage(Messages.getUiMessage(Messages.PARENT_ATTR_NOT_FOUND));
                return "err";
//                throw new Exception("Not found parent attribute with name ("+newAttribute.getParentName()+")");
            }
            else na.setParentAttribute(newAttribute.getParentName());
        //	na.setType(String.class);
        na.setOwner(getNode());
        getNode().addAttr(na);
        na.setRequired(newAttribute.isRequired());
        //	try { na.setValue(""); }
        //	catch(ConstraintException e) { }
        na.init();
        na.save();
        getTree().saveNodeAttribute(na);
        newAttribute.resetMessage();
        log.warn("Added new attribute='{}' for node='{}'", na.getName(), getNode().getName());
        auditor.write(getNode(), getAccountName(), Action.ATTR_CREATE, na.getName());
        onSetNode();
        return "ok";
    }

    public int deleteAttrubute(Attr attr) {
        NodeAttribute na = getNode().getNodeAttribute(attr.getName());
        if (na == null) {
            return -1;
        }
        if (!attr.isAllowDelete()) {
            return -2;
        }
        getNode().removeNodeAttribute(na.getName());
        getConfigurator().getTreeStore().removeNodeAttribute(na.getId());
        log.warn("removed attrubute: {}", na.getName());
        auditor.write(getNode(), getAccountName(), Action.ATTR_DEL, na.getName());
        return 0;
    }

    public void afterDeleteAttrubutes() {
        onSetNode();
        restartNode();
    }

    public void afterWriteAttrubutes() {
        if (this.isStarted()) {
            restartNode();
        }
    }

    public void restartNode() {
        if (getNode().getStatus() == Node.Status.STARTED) {
            getNode().stop();
            getNode().start();
        }
    }

    public NodeWrapper getParent() {
        try {
            return new NodeWrapper(getNode().getParent());
        } catch (NullPointerException e) {
            return null;
        }
    }

    public int getIndex() {
        try {
            return getNode().getIndex();
        } catch (NullPointerException e) {
            return -1;
        }
    }

    public String deleteAttrubutes(List<Attr> attrs) {
        StringBuffer ret = new StringBuffer();
        Iterator<Attr> it = attrs.iterator();
        while (it.hasNext()) {
            Attr a = it.next();
            NodeAttribute na = getNode().getNodeAttribute(a.getName());
            if (na == null) {
                String t = Messages.getUiMessage(Messages.ATTR_NOT_FOUND);
                ret.append(t + a.getName());
                break;
            }
            if (!a.isAllowDelete()) //if(na.getParentAttribute()!=null || na.getParameterName()!=null)
            {
                String t = Messages.getUiMessage(Messages.ATTR_CANT_DEL);
                if (ret.length() == 0) {
                    ret.append(t);
                } else {
                    ret.append(", ");
                }
                ret.append(na.getName());
                continue;
            }
            getNode().removeNodeAttribute(na.getName());
            getConfigurator().getTreeStore().removeNodeAttribute(na.getId());
            log.warn("removed attrubute: {}", na.getName());
            auditor.write(getNode(), getAccountName(), Action.ATTR_DEL, na.getName());
        }
        onSetNode();
        if (ret.length() == 0) {
            return null;
        }
        return ret.toString();
    }

    public boolean equals(Object x) {
        if (x != null && x instanceof NodeWrapper) {
            NodeWrapper xx = (NodeWrapper) x;
            return (this.getNode().equals(xx.getNode()));
        }
        return false;
    }

    /*
     * 	
     */
    public List<NodeWrapper> getChildrenList() {
        ArrayList<NodeWrapper> al = new ArrayList<NodeWrapper>();
        List<Node> lst = getNode().getChildrenList();
        if (lst == null || lst.size() == 0) {
            return al;
        }
        Iterator<Node> it = lst.iterator();
        while (it.hasNext()) {
            NodeWrapper nw = new NodeWrapper(it.next());
            // if(!nw.isAllowTreeEdit()) continue;
            //if(!nw.isAllowNodeRead()) continue;
            if (!nw.isAnyAccess()) {
                continue;
            }
            al.add(nw);
        }
        return al;
    }

    public String getNodeShowType() {
        if (isGraphNode()) {
            return "GraphNode";
        }
        return null;
    }

    public NewAttribute getNewAttribute() {
        return newAttribute;
    }

    public void setNewAttribute(NewAttribute newAttribute) {
        this.newAttribute = newAttribute;
    }

    public static String getNotNull(String x) {
        if (x == null) {
            return "";
        }
        return x;
    }

    public String goToEditNewAttribute(Node n) {
        /*
         showTab.setDisclosed(false);
         nodeEditTab.setDisclosed(true);
         treeEditTab.setDisclosed(true);
         */
        setNode(n);
        return "tabNodeEdit";
    }

    public String onRefresh() {
        return onRefresh2(null, Action.VIEW, "refreshInterval=" + getRefreshViewIntevalMS());
    }

    public String onRefresh2(String message) {
        return onRefresh2("MESSAGE='" + message + "'", Action.VIEW_WITH_ATTR, null);
    }

    public String onRefresh2(String before, Action action, String after) {
        List<Object> data = new ArrayList<Object>();
        if (whoulRefreshAttributes == null) {
            loadRefreshAttributes();
        }
        StringBuilder sb = new StringBuilder();

        if (Utl.trim2Null(before) != null) {
            sb.append(before).append(" ; ");
        }

        for (NodeAttribute a : whoulRefreshAttributes.values()) {
            sb.append("attr='{}' value='{}' ; ");
            data.add(a.getName());
            data.add(a.getValue());
        }
        if (Utl.trim2Null(after) != null) {
            sb.append(after).append(" ;");
        }

        if (data.size() > 0) {
            auditor.write(getNode(), getAccountName(), action, sb.toString(), data.toArray());
        } else {
            auditor.write(getNode(), getAccountName(), action, sb.toString());
        }
        return refresh();
    }

    public String refresh() {
        refreshPressed = true;
        String ret = onRefreshX();
        //refreshPressed = false;
        return ret;
    }

    public String onRefreshX() {
        setNeedRefreshVO();
        SessionBean.getInstance().reloadRightFrame();
        return null;
    }

    public void pollRefresh(PollEvent event) {
        onRefreshX();
    }

    public CoreShowDetailItem getShowTab() {
        return showTab;
    }

    public void setShowTab(CoreShowDetailItem showTab) {
        this.showTab = showTab;
    }

    public CoreShowDetailItem getNodeEditTab() {
        return nodeEditTab;
    }

    public void setNodeEditTab(CoreShowDetailItem nodeEditTab) {
        this.nodeEditTab = nodeEditTab;
    }

    public CoreShowDetailItem getTreeEditTab() {
        return treeEditTab;
    }

    public void setTreeEditTab(CoreShowDetailItem treeEditTab) {
        this.treeEditTab = treeEditTab;
    }

    public CoreShowDetailItem getSelNodeTab() {
        return selNodeTab;
    }

    public void setSelNodeTab(CoreShowDetailItem selNodeTab) {
        this.selNodeTab = selNodeTab;
    }

    public CoreShowDetailItem getSelTemplateTab() {
        return selTemplateTab;
    }

    public void setSelTemplateTab(CoreShowDetailItem selTemplateTab) {
        this.selTemplateTab = selTemplateTab;
    }

    public boolean isUseChildAttributesView() {
        return useChildAttributesView;
    }

    public void setUseChildAttributesView(boolean useChildAttributesView) {
        this.useChildAttributesView = useChildAttributesView;
    }

//	public void setHasUnsavedChanges(boolean hasUnsavedChanges) {
//		this.hasUnsavedChanges = hasUnsavedChanges;
//	}
    public boolean hasUnsavedChanges() {
        if (unsavedChanges.size() > 0) {
            return true;
        }
        return false;
    }

    public boolean hasUnsavedChanges(int attrId) {
        if (unsavedChanges.containsKey(attrId)) {
            return true;
        }
        return false;
    }

    public void clearUnsavedChanges(int attrId) {
        unsavedChanges.remove(new Integer(attrId));
    }

    public void clearAllUnsavedChanges() {
        unsavedChanges.clear();
    }

    public void addUnsavedChanges(int attrId, HashMap<Integer, AuditRecord> aRec) {
        unsavedChanges.put(attrId, aRec);
    }

//	public boolean isHasUnsavedChanges() {
//		return hasUnsavedChanges;
//	}
    public List<NodeAttribute> getReadOnlyAttributes() throws TooManyReferenceValuesException {
        if (readOnlyAttributes == null) {
            loadAttributes();
        }
        return readOnlyAttributes;
    }

    public void setReadOnlyAttributes(List<NodeAttribute> readOnlyAttributes) {
        this.readOnlyAttributes = readOnlyAttributes;
    }

    public List<Attr> getEditingRefreshAttrs() {
        if (editingRefreshAttrs == null) {
            loadRefreshAttributes();
        }
        List<Attr> la = new ArrayList<Attr>(editingRefreshAttrs.values());
        Collections.sort(la);
        return la;
    }

    public void setNeedRefreshVO() {
        this.needRefreshVO = true;
    }

    public void setNeedRefreshVO(boolean needRefreshVO) {
        this.needRefreshVO = needRefreshVO;
    }

    public boolean isNeedRefreshVO() {
        return needRefreshVO;
    }

    public int compare(NodeAttribute o1, NodeAttribute o2) {
        return o1.getName().compareTo(o2.getName());
    }

    private RefreshIntervalCache getRiCache() {
        return SessionBean.getInstance().getRefreshIntervalCache();
    }

    /**
     *
     * @param refreshViewInteval - the interval (seconds)
     */
    public void setRefreshViewInteval(long refreshViewInteval) {
        getRiCache().put(this.getNodeId(), refreshViewInteval * 1000);
    }

    /**
     *
     * @return - the interval (seconds)
     */
    public long getRefreshViewInteval() {
        return getRefreshViewIntevalMS() / 1000;
    }

    /**
     *
     * @return - the interval (milliseconds)
     */
    public long getRefreshViewIntevalMS() {
        return getRiCache().get(this.getNodeId());
    }

    public static boolean isAutoRefresh(Node node) {
        if (node instanceof Viewable) {
            Viewable v = (Viewable) node;
            Boolean b = v.getAutoRefresh();
            if (b != null && b == false) {
                return false;
            }
        }
        return true;
    }

    public boolean isAutoRefresh() {
        return isAutoRefresh(getNode()) && isAutoRefresh(getVoSource());
    }

    public boolean isShowVO() {
        if (isRefreshPressed()) {
            return true;
        }
        return isAutoRefresh();
    }

    public boolean isRefreshPressed() {
        return refreshPressed;
    }

    public void setRefreshPressed(boolean refreshPressed) {
        this.refreshPressed = refreshPressed;
    }

    private void setVoSource(Node voSource) {
        this.voSource = voSource;
        if (voSource == null) {
            voSourceInited = false;
        } else {
            voSourceInited = true;
        }
    }

    public Node getVoSource() {
        if (!voSourceInited) {
            Node n = getNode();
            Node x = null;
            for (int i = 0; i < MAX_VO_SEARCH; i++) {
                x = getNodeByAttr(n, VO_SOURCE);
                if (x == null) {
                    break;
                }
                n = x;
            }
            if (x != null) {
                log.warn("to many VO_SOURCE search steps for node {}", getNodePath());
            }
            setVoSource(n);
        }
        return voSource;
    }

    public NodeWrapper getVoSourceNW() {
        Node n = getVoSource();
        if (n.getId() == getNodeId()) {
            return this;
        }
        NodeWrapper nw = new NodeWrapper(n);
        nw.setRefreshPressed(isRefreshPressed());
        nw.setHideNodeName(isHideNodeName());
        return nw;
    }

    public void setShortNameLen(int shortNameLen) {
        this.shortNameLen = shortNameLen;
    }

    public int getShortNameLen() {
        return shortNameLen;
    }

    public boolean isHasIconPath() {
        IconResource ir = ResourcesCache.getInstance().get(getIconPath());
        if (ir == null) {
            return false;
        }
        return ir.isValid();
    }

    @SuppressWarnings("unchecked")
    public static String classNameToResourceName(Class nc) {
        String clName = nc.getName();
        return clName.replaceAll("\\.", "/");
    }

    @SuppressWarnings("unchecked")
    public String getIconPath() {
        if (iconPath != null) {
            return iconPath;
        }
        ResourcesCache rc = ResourcesCache.getInstance();
        Class cls = getNode().getClass();
        IconResource ra = ResourcesCache.getIconResourceForClass(cls);
        if (ra.isValid()) {
            iconPath = ra.getIconPath();
            rc.put(iconPath, ra);
            return iconPath;
        }
        while (1 == 1) {
            cls = cls.getSuperclass();
            if (!Node.class.isAssignableFrom(cls)) {
                break;
            }
            IconResource rb = ResourcesCache.getIconResourceForClass(cls);
            if (rb.isValid()) {
                iconPath = rb.getIconPath();
                rc.put(iconPath, rb);
                return iconPath;
            }
        }
        iconPath = ra.getIconPath();
        rc.put(iconPath, ra);
        return iconPath;
    }

    public Status getStatus() {
        return getNode().getStatus();
    }

    public boolean isTemplate() {
        return getNode().isTemplate();
    }

    public String getName() {
        return getNode().getName();
    }

    public String getPrefix() {
        return ((BaseNode) getNode()).getPrefix();
    }

    public String getNodeTitle() {
        final NodeAttribute titleAttr = getNode().getAttr("nodeTitle");
        if (titleAttr==null) 
            return null;
        final String title = titleAttr.getValue();
        return title == null || title.isEmpty()? null : " (" + title + ")";
    }

    public boolean isStarted() {
        return getStatus() == Status.STARTED;
    }

    public String getPath() {
        return getNode().getPath();
    }

    public Map<String, NodeAttribute> getWhoulRefreshAttributes() {
        return whoulRefreshAttributes;
    }

    public void setWhoulRefreshAttributes(Map<String, NodeAttribute> whoulRefreshAttributes) {
        this.whoulRefreshAttributes = whoulRefreshAttributes;
    }

    public LogView getLogView() {
        return logView;
    }
    private List<Integer> idList = new ArrayList<Integer>();

    public ScanOperation nodeScanned(Node node) {
        if (SessionBean.getUserContext().getAccessForNode(node) < AccessControl.WRITE) {
            return ScanOperation.SKIP_NODE;
        }
        idList.add(node.getId());
        return ScanOperation.CONTINUE;
    }

    public List<Integer> scan() {
        idList.clear();
        SessionBean.getTree().scanSubtree(getNode(), this, ScanOptionsImpl.EMPTY_OPTIONS);
        return idList;
    }
}
