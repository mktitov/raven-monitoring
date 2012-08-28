package org.raven.ui.log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.raven.log.NodeLogRecord;
import org.raven.ui.AbstractRecordTable;
import org.raven.ui.util.Messages;

public class LogRecordTable extends AbstractRecordTable<NodeLogRecord> {

    private static final long serialVersionUID = 5785110882415569257L;

    protected Object[] getObjects(NodeLogRecord ar) {
        Object[] ret = new Object[]{
            ar.getFdString(),
            ar.getLevel(),
            ar.getNodePath(),
            formMessage(ar.getMessage())
        };
        return ret;
    }
    
    private String formMessage(String message) {
        if (message==null || message.isEmpty())
            return message;
        StringBuilder buf = new StringBuilder();
        for (String line: message.split("\\n"))
            if (line.startsWith("Caused by: "))
                buf.append("<b>").append(line).append("</b>");
            else if (line.contains(".EXPR."))
                buf.append("<b style='color:blue'>").append(line).append("</b>");
            else
                buf.append(line);
        return buf.toString();
    }

    protected String[] initColumnNames() {
        return new String[]{
                    Messages.getUiMessage(Messages.DATE),
                    Messages.getUiMessage(Messages.LEVEL),
                    Messages.getUiMessage(Messages.NODE_PATH),
                    Messages.getUiMessage(Messages.MESSAGE)
                };
    }

    public boolean isDate(int index) {
        if (index == 0) {
            return true;
        }
        return false;
    }
}
