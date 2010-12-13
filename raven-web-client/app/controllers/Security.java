/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import play.Logger;
import play.libs.WS;
import service.App;
import service.RestHelper;

/**
 *
 * @author Mikhail Titov
 */
public class Security extends Secure.Security
{
    public static boolean authenticate(String username, String password)
    {
        try {
            WS.HttpResponse resp = RestHelper.getResponseForRequest("login", username, password, null);
            
            String sessionId = resp.getHeader("Set-Cookie");

            if (sessionId==null)
                return false;

            session.put(App.RAVEN_SESSION_ID, sessionId);
            session.put(App.USERNAME_ATTR, username);
            session.put(App.PASSWORD_ATTR, password);

            Logger.debug("User (%s) successfully loged in", username);

            return true;
            
        } catch (Exception ex)
        {
            return false;
        }

    }
}
