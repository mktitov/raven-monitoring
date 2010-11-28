/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package service;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.io.IOUtils;
import play.Play;
import play.libs.WS;
import play.mvc.Scope;

/**
 *
 * @author Mikhail Titov
 */
public class RestHelper
{
    public static <T> T requestForJson(String path, Scope.Session session, Class<T> beanClass, String... params) throws UnsupportedEncodingException
    {
        String str = request(path, session, params);
        return str==null? null : new Gson().fromJson(str, beanClass);
    }
    
    public static byte[] requestForBinary(String path, Scope.Session session, String... params)
            throws UnsupportedEncodingException, IOException
    {
        InputStream stream =  getResponseForRequest(path, session.get(App.USERNAME_ATTR),
                session.get(App.PASSWORD_ATTR), session, params).getStream();
        if (stream!=null)
            try{
                return IOUtils.toByteArray(stream);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        return null;
    }

    public static String request(String path, Scope.Session session, String... params)
            throws UnsupportedEncodingException
    {
        return getResponseForRequest(path, session.get(App.USERNAME_ATTR),
                session.get(App.PASSWORD_ATTR), session, params).getString();
    }

    public static WS.HttpResponse getResponseForRequest(
            String path, String username, String password, Scope.Session session, String... params)
        throws UnsupportedEncodingException
    {
        StringBuilder url = new StringBuilder(Play.configuration.getProperty(
                App.RAVEN_SERVER_URL_ATTR)).append(path);
        String encoding = Play.configuration.getProperty(App.RAVEN_REST_ENCODING);
        if (params!=null && params.length>0) {
            url.append("?");
            for (int i=0; i<params.length; i+=2){
                if (i>0)
                    url.append("&");
                url.append(params[i]).append('=');
                String val = params[i+1];
                if (val!=null)
                    url.append(URLEncoder.encode(val, encoding));
            }
        }
        WS.WSRequest request = WS.url(url.toString()).authenticate(username, password);
        if (session!=null){
            String ravenSessionId = session.get(App.RAVEN_SESSION_ID);
            if (ravenSessionId!=null)
                request.setHeader("Cookie", ravenSessionId);
        }
        return request.get();
    }

}
