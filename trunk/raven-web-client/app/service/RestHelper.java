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
import java.util.Map;
import org.apache.commons.io.IOUtils;
import play.Logger;
import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Scope;

/**
 *
 * @author Mikhail Titov
 */
public class RestHelper
{
    public static <T> T requestForJson(String path, Scope.Session session, Class<T> beanClass
            , String... params)
        throws Exception
    {
        String str = request(path, session, params);
        return str==null? null : new Gson().fromJson(str, beanClass);
    }
    
    public static byte[] requestForBinary(String path, Scope.Session session, String... params)
            throws Exception
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
            throws Exception
    {
        return getResponseForRequest(path, session.get(App.USERNAME_ATTR),
                session.get(App.PASSWORD_ATTR), session, params).getString();
    }

    public static WS.HttpResponse getResponseForRequest(
            String path, String username, String password, Scope.Session session, String... params)
        throws Exception
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
        HttpResponse response = request.get();
        if (response.getStatus()!=200 && response.getStatus()!=201) {
            Logger.error("Recieved error on request. Status code: %s", response.getStatus());
            throw new Exception(response.getString());
        }
        return response;
    }

    public static String post(String path, Scope.Session session, Map params) throws Exception
    {
        StringBuilder url = new StringBuilder(Play.configuration.getProperty(
                App.RAVEN_SERVER_URL_ATTR)).append(path);
        WS.WSRequest request = WS.url(url.toString())
                .authenticate(session.get(App.USERNAME_ATTR), session.get(App.PASSWORD_ATTR))
                .params(params);

        checkAuth(session, request);
        HttpResponse response = request.post();
        checkResponse(response);

        return response.getString();
    }

    private static void checkAuth(Scope.Session session, WS.WSRequest request)
    {
        if (session!=null){
            String ravenSessionId = session.get(App.RAVEN_SESSION_ID);
            if (ravenSessionId!=null)
                request.setHeader("Cookie", ravenSessionId);
        }
    }

    private static void checkResponse(HttpResponse response) throws Exception
    {
        if (response.getStatus()!=200 && response.getStatus()!=201) {
            Logger.error("Recieved error on request. Status code: %s", response.getStatus());
            throw new Exception(response.getString());
        }
    }
}
