package org.raven.ui;

import org.raven.log.NodeLogRecord;
import org.raven.ui.util.Messages;

public class LogRecordTable extends AbstractRecordTable<NodeLogRecord> 
{
	private static final long serialVersionUID = 5785110882415569257L;

	protected Object[] getObjects(NodeLogRecord ar) {
    	Object[] ret = new Object[] {
    			ar.getFdString(),
    			ar.getNodeId(),
    			ar.getNodePath(),
  //  			ar.getLogin(),
  //  			ar.getActionType(),
   // 			ar.getAction(),
  //  			ar.getMessage()
    	};
		return ret;
	}

	protected String[] initColumnNames() {
		return new String[] { 
				Messages.getUiMessage(Messages.DATE),
				Messages.getUiMessage(Messages.NODE_ID),
				Messages.getUiMessage(Messages.NODE_PATH),
				Messages.getUiMessage(Messages.LOGIN),
				Messages.getUiMessage(Messages.ACTION_TYPE),
				Messages.getUiMessage(Messages.ACTION),
				Messages.getUiMessage(Messages.MESSAGE)
				};
	}	

}
