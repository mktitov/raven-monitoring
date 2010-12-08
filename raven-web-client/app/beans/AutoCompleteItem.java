/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package beans;

import org.raven.rest.beans.NodeTypeBean;

/**
 *
 * @author Mikhail Titov
 */
public class AutoCompleteItem
{
    public String label;
    public String value;
    public String desc;
    public String icon;

    public AutoCompleteItem() {
    }

    public AutoCompleteItem(String label, String value, String desc, String icon)
    {
        this.label = label;
        this.value = value;
        this.desc = desc;
    }

    public AutoCompleteItem(NodeTypeBean nodeType)
    {
        this.label = nodeType.type;
        this.value = nodeType.type;
        this.desc = nodeType.shortDescription;
        this.icon = nodeType.iconPath;
    }
}
