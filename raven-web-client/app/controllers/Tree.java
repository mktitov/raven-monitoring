/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import beans.AutoCompleteItem;
import org.raven.rest.beans.NodeTypeBean;
import java.io.InputStream;
import beans.JsTreeNode;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import org.raven.rest.beans.NodeBean;
import play.Logger;
import play.Play;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.With;
import service.App;
import static service.RestHelper.*;

/**
 *
 * @author Mikhail Titov
 */
@With(Secure.class)
public class Tree extends Controller
{
    public static void index()
    {
        render();
    }

    public static void childs(String path) throws UnsupportedEncodingException
    {
        Logger.debug("Reading child nodes for path (%s)", path);
        path = path==null || path.isEmpty()? null : path;
        NodeBean[] beans = requestForJson("node/childs", session, NodeBean[].class, "path", path);
        JsTreeNode[] nodes = null;
        if (beans!=null){
            nodes = new JsTreeNode[beans.length];
            for (int i=0; i<beans.length; ++i) 
                nodes[i] = new JsTreeNode(beans[i]);
        }
        renderJSON(nodes);
    }

    public static void icon(String path) throws Exception
    {
        Logger.debug("Reading icon (%s)", path);
        byte[] image = requestForBinary("node/icon", session, "path", path);
        if (image!=null){
            InputStream stream = new ByteArrayInputStream(image);
            renderBinary(stream, image.length);
        }else
            badRequest();
    }

    public static void childNodeTypes(String path) throws UnsupportedEncodingException
    {
        Logger.debug("Reading child node types for path (%s)", path);
        NodeTypeBean[] beans = requestForJson("node/child-node-types", session, NodeTypeBean[].class, "path", path);
        if (beans==null || beans.length==0)
            renderJSON(null);
        else {
            AutoCompleteItem[] items = new AutoCompleteItem[beans.length];
            for (int i=0; i<beans.length; ++i)
                items[i]=new AutoCompleteItem(beans[i]);
            renderJSON(items);
        }
    }

    public static void createNewNode(String parent, String name, String type)
    {
        Logger.debug("Creating new node: parent=%s, name=%s, type=%s", parent, name, type);
    }

    public static void testRead() throws UnsupportedEncodingException
    {
        String nodes = request("node/childs", session, "path", "System");
        renderText(nodes);
    }

    public static void testReadJson() throws UnsupportedEncodingException
    {
        NodeBean[] beans = requestForJson("node/childs", session, NodeBean[].class);
        JsTreeNode[] nodes = new JsTreeNode[beans.length];
        for (int i=0; i<beans.length; ++i)
            nodes[i] = new JsTreeNode(beans[i]);
        
        renderJSON(nodes);
    }

    public static void showCookie() throws UnsupportedEncodingException
    {
//        String ravenSession = session.get("raven.server.session");
//        WS.WSRequest req = WS.url("http://localhost:8080/rest/helloworld");
//        req.authenticate("root", "12345");
//        if (ravenSession!=null)
//            req.setHeader("Cookie", ravenSession);
//        WS.HttpResponse resp = req.get();
        String username = session.get(App.USERNAME_ATTR);
        String password = session.get(App.PASSWORD_ATTR);
        Logger.debug("Executing request for user (%s) with password (%s)", username, password);
        
        WS.HttpResponse resp = getResponseForRequest("helloworld", username, password, session);
        StringBuilder text = new StringBuilder("Headers: \n");
        for (Header header: resp.getHeaders())
            text.append(header.name).append(": ").append(header.value()).append("\n");
        text.append("\nText:\n").append(resp.getString()).append("\n");
        text.append("Username: ");
//        text.append("\nRaven session: ").append(ravenSession).append("\n");
//        if (ravenSession==null){
//            ravenSession = resp.getHeader("Set-Cookie");
//            session.put("raven.server.session", ravenSession);
//            text.append("\nCreated raven session: ").append(ravenSession).append("\n");
//        }
        renderText(text.toString());
    }

    public static void readConfig()
    {
        renderText(Play.configuration.getProperty("module.maven"));
    }
}
