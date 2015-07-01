package org.raven.ui.util;

import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

public abstract class UIUtil 
{
	@SuppressWarnings("unchecked")
	public static SelectItem[] makeSI(Enum[] values, boolean needNull)
	{
		ArrayList<SelectItem> si = new ArrayList<SelectItem>();
		if(needNull) 
			si.add(new SelectItem(null,"-----"));
		for(Enum x : values)
			si.add(new SelectItem(x));
		return si.toArray(new SelectItem[]{});
	}

	public static SelectItem[] findCharsets()
	{
		ArrayList<Charset> t = new ArrayList<Charset>();
		FacesContext fc = FacesContext.getCurrentInstance(); 
		ExternalContext ec = fc.getExternalContext();
		String x = ec.getRequestHeaderMap().get("Accept-Charset");
		StringBuffer sb = new StringBuffer();
		if(x!=null) sb.append(x);
		
		if(sb.length()>0) sb.append(",");
		sb.append(Messages.getUiMessage(Messages.CHARSET1));
		sb.append(",").append(Messages.getUiMessage(Messages.CHARSET2));
		sb.append(",").append(Messages.getUiMessage(Messages.CHARSET3));
		sb.append(",").append(Messages.getUiMessage(Messages.CHARSET4));
		x = ec.getRequestCharacterEncoding();
		if(x!=null) sb.append(",").append(x);
		
		x = sb.toString();
		String charset;
        String[] charsets = x.split("\\s*,\\s*");
        if (charsets!=null && charsets.length>0)
          	for(String z : charsets)
           	{
           		charset = z.split(";")[0];
           		try {
           			Charset ch = Charset.forName(charset);
           			if(!t.contains(ch))
       				t.add(ch);
           		}	catch(Exception e) {}
          	}
        
		ArrayList<SelectItem> si = new ArrayList<SelectItem>();
		for(Charset ch : t)
			si.add(new SelectItem(ch));
        if(si.size()==0) 
        	si.add( new SelectItem(Charset.forName("UTF-8")) );
        return si.toArray(new SelectItem[]{});
	}
	
}
