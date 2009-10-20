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
	
/*	
	public String getX0() { return getXI(0); }
	public String getX1() { return getXI(1); }
	public String getX2() { return getXI(2); }
	public String getX3() { return getXI(3); }
	public String getX4() { return getXI(4); }
	public String getX5() { return getXI(5); }
	public String getX6() { return getXI(6); }
	public String getX7() { return getXI(7); }
	public String getX8() { return getXI(8); }
	public String getX9() { return getXI(9); }

	public String getX10() { return getXI(10); }
	public String getX11() { return getXI(11); }
	public String getX12() { return getXI(12); }
	public String getX13() { return getXI(13); }
	public String getX14() { return getXI(14); }
	public String getX15() { return getXI(15); }
	public String getX16() { return getXI(16); }
	public String getX17() { return getXI(17); }
	public String getX18() { return getXI(18); }
	public String getX19() { return getXI(19); }
	public String getX20() { return getXI(20); }

	public String getX21() { return getXI(21); }
	public String getX22() { return getXI(22); }
	public String getX23() { return getXI(23); }
	public String getX24() { return getXI(24); }
	public String getX25() { return getXI(25); }
	public String getX26() { return getXI(26); }
	public String getX27() { return getXI(27); }
	public String getX28() { return getXI(28); }
	public String getX29() { return getXI(29); }
	
*/	
	
}
