/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package beans;

import java.util.LinkedHashMap;
import java.util.Map;
import org.raven.rest.beans.NodeBean;
import play.mvc.Router;

/**
 *
 * @author Mikhail Titov
 */
public class JsTreeNode
{
    public Map<String, String> data = new LinkedHashMap<String, String>();
    public String state;
    public Map<String, Object>  attr = new LinkedHashMap<String, Object>();

    public JsTreeNode(NodeBean nodeBean)
    {
        data.put("title", nodeBean.name);
        data.put("icon", Router.reverse("Tree.icon").add("path", nodeBean.iconPath).url);
        this.state = nodeBean.hasChilds? "closed" : null;
        attr.put("id", nodeBean.id);
        attr.put("rights", nodeBean.rights);
        attr.put("path", nodeBean.path);
        attr.put("type", nodeBean.type);
    }
}

