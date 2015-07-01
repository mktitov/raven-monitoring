package org.raven.store;

public interface IRecord {

	public long getFd(); 
	
	public Object[] getDataForInsert(); 
	
//	public IRecord getObjectFromRecord(ResultSet rs) throws SQLException;
	
}
