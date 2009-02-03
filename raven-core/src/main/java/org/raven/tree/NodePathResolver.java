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
}
