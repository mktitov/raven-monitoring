/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package controllers;

import play.mvc.Controller;

/**
 *
 * @author Mikhail Titov
 */
public class Tree extends Controller
{
    public static void index()
    {
        render();
    }

    public static void node(){
        renderJSON("[{\"data\" : \"A node\",\"children\" : [ { \"data\" : \"Only child\", \"state\" : \"closed\" }, \"Child 2\" ]}]");
    }
}
