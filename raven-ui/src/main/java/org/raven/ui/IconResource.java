package org.raven.ui;

public class IconResource 
{
	public static final String MIME_PREFIX =  "image/";
	public static final String RES_SIGN = "~zrrsz~/"; 
	public static final String RES_SIGN2 = "/"+RES_SIGN; 
	private String mimeType;
	private String type;
	private byte[] data;
	private boolean valid = false;
	private String iconPath;
	
	public IconResource(String t, byte[] d, String cr)
	{
		type = t;
		mimeType = MIME_PREFIX + t;
		data = d;
		if(t!=null && t.length()>0 && d!=null)
		{
			iconPath = RES_SIGN + cr + "." + t;
			valid = true;
		}	
		else iconPath = RES_SIGN + cr;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getType() {
		return type;
	}
	
	public byte[] getData() {
		return data;
	}

	public boolean isValid() {
		return valid;
	}

	public String getIconPath() {
		return iconPath;
	}
	
}
