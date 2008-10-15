package org.raven.log;

import java.sql.Date;

public class MetaTableRecord 
{
	private Date fd;
	private Date td;
	private String name;
	
	public Date getFd() {
		return fd;
	}
	public void setFd(Date fd) {
		this.fd = fd;
	}
	public Date getTd() {
		return td;
	}
	public void setTd(Date td) {
		this.td = td;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}
