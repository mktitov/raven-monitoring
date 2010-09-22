package org.raven.audit;

public enum Action 
{

	ATTR_CREATE(ActionType.NODE_EDIT), ATTR_DEL(ActionType.NODE_EDIT), 
	ATTR_RENAME(ActionType.NODE_EDIT), ATTR_CH_VALUE(ActionType.NODE_EDIT),
	ATTR_CH_TYPE(ActionType.NODE_EDIT), ATTR_CH_SUBTYPE(ActionType.NODE_EDIT),
	ATTR_CH_DSC(ActionType.NODE_EDIT),
	
	NODE_CREATE(ActionType.TREE_EDIT), NODE_DEL(ActionType.TREE_EDIT),
	NODE_COPY(ActionType.TREE_EDIT), NODE_MOVE(ActionType.TREE_EDIT), 
	NODE_RENAME(ActionType.TREE_EDIT), NODE_CH_INDEX(ActionType.TREE_EDIT),
	NODES_IMPORT(ActionType.TREE_EDIT), NODES_EXPORT(ActionType.TREE_EDIT),
	
	NODE_START(ActionType.CONTROL), NODE_STOP(ActionType.CONTROL),
	NODE_START_RECURSIVE(ActionType.CONTROL),
	
	SESSION_START(ActionType.SESSION),SESSION_STOP(ActionType.SESSION),
	
	VIEW(ActionType.VIEW),
	VIEW_WITH_ATTR(ActionType.VIEW),
	
	ACTION(ActionType.ACTION),
	ACTION_WITH_ATTR(ActionType.ACTION)		;
	
	private final ActionType actionType;
	
	private Action(ActionType at)
	{
		actionType = at;
	}
	
	public ActionType getActionType() {
		return actionType;
	}
	
}
