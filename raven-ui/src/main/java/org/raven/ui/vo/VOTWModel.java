package org.raven.ui.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.myfaces.trinidad.model.SortCriterion;
import org.apache.myfaces.trinidad.model.SortableModel;

// TableItemWrapper[]

public class VOTWModel extends SortableModel implements Comparator<TIWList>
{
	private int columns = 0;
	//private ArrayList<TableItemWrapper[]> data;
	private ArrayList<TIWList> data;
	private ArrayList<SortCriterion> scList = new ArrayList<SortCriterion>();
	
	//public VOTWModel(ArrayList<TableItemWrapper[]> x)
	public VOTWModel(ArrayList<TIWList> x)
	{
		setWrappedData(x);
		data = x;
		if(x!=null && x.size()>0)
		{
			//columns = x.get(0).length;
			columns = Math.min(x.get(0).size(),VOTableWrapper.MAX_COLUMNS);
		}	
	}

	//ArrayList<TableItemWrapper[]>

	private int getColNum(String property)
	{
		try {
			int i = Integer.parseInt(property);
			if(i >= columns) 
				return -1; 
			return i;
		} catch(Exception e) { 
			return -1; 
		}
	}
	
	public boolean isSortable(String property)
	{
		if( getColNum(property) > 0 ) return true;
		return false; 
	}
	
	public void setSortCriteria(List<SortCriterion> criteria)
	{
		scList.clear();
		for(SortCriterion x : criteria)
			scList.add(x);
		Collections.sort(data, this);
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
		for(SortCriterion sc : scList)
		{
			int cn = getColNum(sc.getProperty());
			if(cn<0) continue;
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
