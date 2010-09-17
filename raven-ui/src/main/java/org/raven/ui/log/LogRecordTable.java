package org.raven.ui.log;

import org.raven.log.NodeLogRecord;
import org.raven.ui.AbstractRecordTable;
import org.raven.ui.util.Messages;

public class LogRecordTable extends AbstractRecordTable<NodeLogRecord> 
{
	private static final long serialVersionUID = 5785110882415569257L;

	protected Object[] getObjects(NodeLogRecord ar) {
    	Object[] ret = new Object[] {
    			ar.getFdString(),
    			ar.getLevel(),
    			ar.getNodePath(),
    			ar.getMessage()
    	};
		return ret;
	}

	protected String[] initColumnNames() {
		return new String[] { 
				Messages.getUiMessage(Messages.DATE),
				Messages.getUiMessage(Messages.LEVEL),
				Messages.getUiMessage(Messages.NODE_PATH),
				Messages.getUiMessage(Messages.MESSAGE)
		};
	}

	public boolean isDate(int index) {
		if(index==0) return true;
		return false;
	}	

}
