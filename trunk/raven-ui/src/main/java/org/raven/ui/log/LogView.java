package org.raven.ui.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.model.SelectItem;
import org.raven.log.LogLevel;
import org.raven.log.NodeLogRecord;
import org.raven.log.NodeLogger;
import org.raven.ui.node.INodeScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weda.internal.annotations.Service;

public class LogView {

    protected static final Logger log = LoggerFactory.getLogger(LogView.class);
    public static final int ALL_NODES = -1;
    public static final SelectItem[] logsSI = {
        new SelectItem(LogLevel.TRACE),
        new SelectItem(LogLevel.DEBUG),
        new SelectItem(LogLevel.INFO),
        new SelectItem(LogLevel.WARN),
        new SelectItem(LogLevel.ERROR)
    };
    @Service
    private NodeLogger nodeLogger;
    private LogViewAttributes attrs;
    private int nodeId;
    private INodeScanner scanner;
    private LogRecordTable logRecordTable = new LogRecordTable();
//	private boolean reload = false;

    public LogView(LogViewAttributes lvAttrs, int nodeId, INodeScanner scan) {
        attrs = lvAttrs;
        this.nodeId = nodeId;
        scanner = scan;
    }

    public SelectItem[] getLogLevelSelectItems() {
        return logsSI;
    }

    protected List<NodeLogRecord> getLogRecords(Object nodeId) {
        Date fd = new Date(attrs.getFdTime());
        Date td = new Date(attrs.getTdTime());
        return nodeLogger.getRecords(fd, td, nodeId, attrs.getLevel());
    }

    private List<NodeLogRecord> getLogsForNodes(Object nodeId) {
        List<NodeLogRecord> ret = getLogRecords(nodeId);
        if (ret.size() > 0 && ret.get(0) != null) {
            Collections.sort(ret, ret.get(0));
        }
        return ret;
    }

    private List<NodeLogRecord> getLogsForNode() {
        if (logRecordList != null) {
            return logRecordList;
        }
        List<NodeLogRecord> res;
        List<Integer> idList = null;
        Object nid = null;
        if (nodeId != ALL_NODES) {
            if (!isFindRecursive()) {
                nid = new Integer(nodeId);
            } else if (scanner != null) {
                idList = scanner.scan();
                if (idList.size() < 200) {
                    idList.add(0, nodeId);
                    nid = idList;                    
                    idList = null;
                } else {
                    nid = null;
                }
            }
        }
        res = getLogsForNodes(nid);
        if (idList != null) {
            Iterator<NodeLogRecord> it = res.iterator();
            while (it.hasNext()) {
                NodeLogRecord r = it.next();
                if (!idList.contains(r.getNodeId())) {
                    it.remove();
                }
            }
        }
        logRecordList = filterLogRecords(res);
        return logRecordList;
    }
    private List<NodeLogRecord> logRecordList = null;

    public String loadLogForNodes() {
        logRecordTable.clear();
        logRecordList = null;
        //LogsCache lc = SessionBean.getLogsCacheS();
        //lc.remove(nodeId);
        //List<NodeLogRecord> lst = lc.get(nodeId); 
        //if(lst!=null) 
//		reload = true;
        logRecordTable.addAll(getLogsForNode());
//		reload = false;
        return null;
    }

    public List<LogByNode> getLogsGroupedByNodes() {
        return (new LogsByNodes(getLogsForNode())).getAll();
    }

//	public String clearLogForNode()
//	{
//		SessionBean.getLogsCacheS().remove(nodeId);
//		return null;
//	}
    public String getFd() {
        return attrs.getFd();
    }

    public void setFd(String fd) {
        attrs.setFd(fd);
    }

    public String getTd() {
        return attrs.getTd();
    }

    public void setTd(String td) {
        attrs.setTd(td);
    }

    public LogLevel getLevel() {
        return attrs.getLevel();
    }

    public void setLevel(LogLevel level) {
        attrs.setLevel(level);
    }

    public List<NodeLogRecord> filterByRegExp(List<NodeLogRecord> res, String regExp) {
        List<NodeLogRecord> ret = new ArrayList<NodeLogRecord>();
        Pattern p = Pattern.compile(regExp);
        for (NodeLogRecord r : res) {
            Matcher m = p.matcher(r.getMessage());
            if (m.find()) {
                ret.add(r);
            }
        }
        return ret;
    }

    public List<NodeLogRecord> filterByString(List<NodeLogRecord> res, String pattern) {
        List<NodeLogRecord> ret = new ArrayList<NodeLogRecord>();
        for (NodeLogRecord r : res) {
            if (r.getMessage().indexOf(pattern) != -1) {
                ret.add(r);
            }
        }
        return ret;
    }

    public void setGroupByNodes(boolean val) {
        attrs.setGroupByNodes(val);
    }

    public boolean isGroupByNodes() {
        return attrs.isGroupByNodes();
    }

    public String getMessageFilter() {
        return attrs.getMesFilter();
    }

    public void setMessageFilter(String f) {
        attrs.setMesFilter(f);
    }

    public boolean isFilterRegExp() {
        return attrs.isRegExp();
    }

    public void setFilterRegExp(boolean f) {
        attrs.setRegExp(f);
    }

    public boolean isFindRecursive() {
        return attrs.isFindRecursive();
    }

    public void setFindRecursive(boolean f) {
        attrs.setFindRecursive(f);
    }

    public boolean isFilterOn() {
        return attrs.isFilterOn();
    }

    public void setFilterOn(boolean f) {
        attrs.setFilterOn(f);
    }

    public String filterEnable() {
        setFilterOn(true);
        return null;
    }

    public String filterDisable() {
        setFilterOn(false);
        return null;
    }

    public List<NodeLogRecord> filterLogRecords(List<NodeLogRecord> res) {
        String mf = getMessageFilter().trim();
        if (isFilterOn() && !mf.isEmpty()) {
            if (isFilterRegExp()) {
                res = filterByRegExp(res, mf);
            } else {
                res = filterByString(res, mf);
            }
        }
        return res;
    }

    public LogRecordTable getLogRecordTable() {
        return logRecordTable;
    }
}
