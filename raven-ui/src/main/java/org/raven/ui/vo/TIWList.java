package org.raven.ui.vo;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//extends TableItemWrapper[]

public class TIWList extends ArrayList<TableItemWrapper>  
{
	private static final long serialVersionUID = -1269069921894205502L;
	private static final Logger logger = LoggerFactory.getLogger(TIWList.class);	

	public String getXI(int i)
	{
		if(i >= this.size()) return "";
		return get(i).getString();
	}
	
	public TableItemWrapper get(int i)
	{
		try {
		//	logger.warn("on get: "+i);
			return super.get(i);
		} 
		catch(IndexOutOfBoundsException e){
			logger.warn("on get: IndexOutOfBoundsException");
			return null;
		}
	}

	public boolean isValid(int i)
	{
		try {
				super.get(i);
				return true;
			} 
			catch(IndexOutOfBoundsException e){
				return false;
			}
		
	}
}
