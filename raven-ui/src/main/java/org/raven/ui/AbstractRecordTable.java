package org.raven.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import org.raven.table.Table;
import org.raven.table.TableTag;

public abstract class AbstractRecordTable<T> extends ArrayList<T> implements Table 
{
	private static final long serialVersionUID = 6536815712846251464L;
	private String[] colNames;
	private String title = "";

	protected abstract String[] initColumnNames();
	protected abstract Object[] getObjects(T t);
	
	public AbstractRecordTable()
	{
		super();
		init();
	}
/*	
	public AbstractRecordTable(Collection<T> list)
	{
		super(list);
		init();
	}
*/
	private void init()
	{
		colNames = initColumnNames();
	}
	
	public boolean containsColumnTag(int col, String tagId) {
		return false;
	}

	public boolean containsRowTag(int row, String tagId) {
		return false;
	}

	public int getColumnIndex(String columnName) {
		for(int i=0; i< colNames.length; i++)
			if(columnName.equals(colNames[i]))
				return i;
		return 0;
	}

	public String[] getColumnNames() {
		return colNames;
	}

	public Map<String, TableTag> getColumnTags(int col) {
		return null;
	}

	public Iterator<Object[]> getRowIterator() {
		return new ARTIterator();
	}

	public Map<String, TableTag> getRowTags(int row) {
		return null;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String t) {
		title = t;
	}
	
    private class ARTIterator implements Iterator<Object[]>
    {
        private int currentRow = 0;

        public boolean hasNext() {
            return currentRow<size();
        }

        public Object[] next()
        {
        	T t = get(currentRow++);
        	Object[] ret = getObjects(t); 
        	return ret;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

    }	
	
}
