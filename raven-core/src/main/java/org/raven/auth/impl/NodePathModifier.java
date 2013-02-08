package org.raven.auth.impl;

import org.raven.auth.impl.AccessControl;

public enum NodePathModifier 
{
	NODE_ONLY (AccessControl.NODE),
	CHILDREN_ONLY (AccessControl.CHILDREN), 
	NODE_and_CHILDREN (AccessControl.NODE_AND_CHILDREN);
	
	private final String modifier;
	
	private NodePathModifier(String modifier) {
		this.modifier = modifier;
	}

	public String getModifier() {
		return modifier;
	}
}
