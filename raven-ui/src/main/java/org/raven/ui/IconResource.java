package org.raven.ui;

public class IconResource 
{
	private String mimeType;
	private byte[] data;
	private boolean valid = false;
	
	public IconResource(String mt, byte[] d)
	{
		mimeType = mt;
		data = d;
		if(mt!=null && mt.length()>0 && d!=null) 
			valid = true;
	}

	public String getMimeType() {
		return mimeType;
	}

	public byte[] getData() {
		return data;
	}

	public boolean isValid() {
		return valid;
	}
	
}
