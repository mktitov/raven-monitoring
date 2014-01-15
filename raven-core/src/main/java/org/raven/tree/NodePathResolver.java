/*
 *  Copyright 2008 Mikhail Titov.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.raven.tree;

import java.util.List;

/**
 * Allows to resolve the node by path.
 * @author Mikhail Titov
 */
public interface NodePathResolver 
{
    public final static char QUOTE = '"';
    public final static String PARENT_REFERENCE = "..";
    public final static String SELF_REFERENCE = ".";
    
    /**
     * Splits the path passed in the parameter to the path elements
     * @param path string representation of the path
     */
    public List<String> splitToPathElements(String path);
    /**
     * Resolve path relative (if the path type is relative) from node passed 
     * in the parameter <code>currentNode</code>.
     * The format of the path is
     * <pre>
     *  ref = ("node_name"|node_name|..|.)
     *  path = ref[/path]
     * </pre>
     * @param path the path to the node relative to the <code>currentNode</code> if path is
     *      relative
     * @param currentNode the starting node for the expression passed in the <code>path</code>
     *      parameter if the type of the path is relative.
     */
    public PathInfo resolvePath(String path, Node currentNode) throws InvalidPathException;
    /**
     * Returns the absolute path to the node passed in the parameter.
     */
    public String getAbsolutePath(Node node);
    /**
     * Returns the relative path from the node <b>fromNode</b> to the node <b>toNode</b>.
     * @param fromNode the begin of the path
     * @param toNode the target of the path
     */
    public String getRelativePath(Node fromNode, Node toNode);
    /**
     * Returns the relative path from the node to the attribute of another node
     */
    public String getRelativePath(Node fromNode, NodeAttribute toAttr);
    /**
     * Return <b>true</b> if the path is absolute path or <b>false</b> if the path is relative.
     * @param path the path to the node
     * @return
     */
    public boolean isPathAbsolute(String path);
    /**
     * Returns the absolute path to the node attribue passed in the parameter.
     */
    public String getAbsolutePath(NodeAttribute attribute);
    /**
     * Create path for node names passed in parameter
     * @param nodeNames the array of the node names
     * @param absolute if true then method creates the absolute path else relative path
     *      will be created
     */
    public String createPath(boolean absolute, String... nodeNames);
    /**
     * Creates path from source node to the target node
     * @param fromNode source node (
     *      the information about source node is ignoring when <i>absolute==true</i>)
     * @param toNode the target node
     * @param absolute if true then absolute path will be created, else relative path will
     *      be created
     * @return
     */
//    public String createPath(Node fromNode, Node toNode, boolean absolute);
}
