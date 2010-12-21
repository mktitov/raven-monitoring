/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import java.util.Map;
import beans.AutoCompleteItem;
import org.raven.rest.beans.NodeTypeBean;
import java.io.InputStream;
import beans.JsTreeNode;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import org.apache.commons.lang.StringUtils;
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

    public static void nodeTypes() throws Exception
    {
        Logger.debug("Loading node types");
        NodeTypeBean[] beans = requestForJson("node/node-types", session, NodeTypeBean[].class);
        renderJSON(beans);
    }

    public static void throughNodeTypes() throws Exception
    {
        Logger.debug("Loading through node types");
        String[] types = requestForJson("node/through-node-types", session, String[].class);
        renderJSON(types);
    }

    public static void childs(String path) throws Exception
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

    public static void childNodeTypes(String path) throws Exception
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
        try {
            String path = request("node/create-node", session, "parent", parent, "name", name, "type", type);
            renderText(path);
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    public static void deleteNodes(String[] nodes)
    { 
        Logger.debug("Received delete request for nodes: %s", nodes.length);
        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put("nodes", nodes);
        try{
            post("node/delete-nodes", session, requestParams);
            ok();
        } catch (Exception e){
            error(e.getMessage());
        }
    }

    public static void moveNodes(String destination, String[] nodes, int position)
    {
        Logger.debug("Moving nodes (%s) to the new location (%s)"
                , StringUtils.join(nodes, ", "), destination);
        Map<String, Object> requestParams = new HashMap<String, Object>();
        requestParams.put("destination", destination);
        requestParams.put("nodes", nodes);
        requestParams.put("position", position);
        try{
            post("node/move-nodes", session, requestParams);
            ok();
        } catch(Exception e) {
            error(e.getMessage());
        }
    }

    public static void testRead() throws Exception
    {
        String nodes = request("node/childs", session, "path", "System");
        renderText(nodes);
    }

    public static void testReadJson() throws Exception
    {
        NodeBean[] beans = requestForJson("node/childs", session, NodeBean[].class);
        JsTreeNode[] nodes = new JsTreeNode[beans.length];
        for (int i=0; i<beans.length; ++i)
            nodes[i] = new JsTreeNode(beans[i]);
        
        renderJSON(nodes);
    }

    public static void showCookie() throws Exception
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
