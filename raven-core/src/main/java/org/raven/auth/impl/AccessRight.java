package org.raven.auth.impl;

public enum AccessRight 
{
	NONE (""+AccessControl.NONE_SYMBOL),
	VISIBLE (""+AccessControl.VIEW_SYMBOL),
	READ (""+AccessControl.READ_SYMBOL),
	WRITE (""+AccessControl.WRITE_SYMBOL),
	CONTROL (""+AccessControl.CONTROL_SYMBOL),
	TREE_EDIT (""+AccessControl.TREE_EDIT_SYMBOL),
	WRITE_and_CONTROL (""+AccessControl.WRITE_SYMBOL+AccessControl.CONTROL_SYMBOL),
	TREE_EDIT_and_CONTROL (""+AccessControl.TREE_EDIT_SYMBOL+AccessControl.CONTROL_SYMBOL);
	
	private final String rights;
	
	private AccessRight(String rights)
	{
		this.rights = rights;
	}

	public String getRights() 
	{
		return rights;
	}
    
    public int getDecodedRights() {
        return AccessControl.decodeRight(rights);
    }
}
