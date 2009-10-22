package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.myfaces.trinidad.model.SortCriterion;
import org.apache.myfaces.trinidad.model.SortableModel;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

// TableItemWrapper[]

public class VOTWModel extends SortableModel implements Comparator<TIWList>
{
 //   private static final Logger logger = LoggerFactory.getLogger(VOTWModel.class);	
	private int columns = 0;
	//private ArrayList<TableItemWrapper[]> data;
	private ArrayList<TIWList> data;
	private ArrayList<SortCriterion> scList = new ArrayList<SortCriterion>();
//	private String name = "noName";
	
	//public VOTWModel(ArrayList<TableItemWrapper[]> x) VOTableWrapper
	//public VOTWModel(ArrayList<TIWList> x)
	public VOTWModel(VOTableWrapper x)
	{
		setWrappedData(x);
		data = x;
		if(x!=null && x.size()>0)
		{
			//columns = x.get(0).length;
			int a = x.get(0).size();
			if(VOTableWrapper.addCounter) a++;
			columns = Math.min(a,VOTableWrapper.MAX_COLUMNS);
		}	
//		name = this.toString();
		//logger.warn("new VOTWModel: "+name+" , columns="+columns+" , tWrapper="+x.toString());
	}

	//ArrayList<TableItemWrapper[]>

	private int getColNum(String property)
	{
		try {
			int i = Integer.parseInt(property);
			if(i >= columns || i<0) 
				return -1; 
			return i;
		} catch(Exception e) { 
			return -1; 
		}
	}
	
	public boolean isSortable(String property)
	{
		if( getColNum(property) >= 0 ) return true;
		return false; 
	}
	
	public void setSortCriteria(List<SortCriterion> criteria)
	{
		scList.clear();
	//	logger.warn("VOTWModel: "+name+" setSortCriteria:"+criteria.get(0).getProperty()+" crCount:"+criteria.size());
		for(SortCriterion x : criteria)
			scList.add(x);
		Collections.sort(data, this);
	///	logger.warn("VOTWModel: "+name+" setSortCriteria ok, data.size="+data.size());
	}

	public List<SortCriterion> getSortCriteria()	
	{
		return scList;
	}

	private int compareAsDouble(String s1, String s2)
	{
		try {
		Double d = Double.parseDouble(s1);
		return d.compareTo(Double.parseDouble(s2));
		} catch(NumberFormatException e) { return -2; }
	}
/*	
	public int compare(TableItemWrapper[] o1, TableItemWrapper[] o2) 
	{
		for(SortCriterion sc : scList)
		{
			int cn = getColNum(sc.getProperty());
			if(cn<0) continue;
			String s1 = o1[cn].getString();
			String s2 = o2[cn].getString();
			int c = compareAsDouble(s1, s2);
			if(c==-2)
				c = s1.compareTo(s2);
			int asc = sc.isAscending() ? 1 : -1;
			if(c!=0) return c*asc;
		}
		return 0;
	}
*/
	public int compare(TIWList o1, TIWList o2) 
	{
//		logger.warn("sort!");
		if(o1==null) return 0;
		if(o2==null) return 0;
		for(SortCriterion sc : scList)
		{
			int cn = getColNum(sc.getProperty());
			if(cn<0 || cn>=o1.size()) continue;
			String s1 = o1.get(cn).getString();
			String s2 = o2.get(cn).getString();
			int c = compareAsDouble(s1, s2);
			if(c==-2)
				c = s1.compareTo(s2);
			int asc = sc.isAscending() ? 1 : -1;
			if(c!=0) return c*asc;
		}
		return 0;
	}
	
}
