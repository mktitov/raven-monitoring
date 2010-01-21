/*
 *  Copyright 2009 Mikhail Titov.
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

package org.raven.tree.impl;

import java.util.Collection;
import org.raven.log.LogLevel;
import org.raven.tree.ActionViewableObject;
import org.raven.tree.Node;
import org.raven.tree.NodeAttribute;
import org.raven.tree.Viewable;
import org.weda.internal.annotations.Message;

/**
 *
 * @author Mikhail Titov
 */
public abstract class AbstractActionViewableNode implements ActionViewableObject
{
    private final String confirmationMessage;
    private final String displayMessage;
    private final Node owner;

    @Message
    private static String actionExecutionErrorMessage;

    /**
     * @param confirmationMessage The string or message key of the confirmation message
     * @param displayMessage The string or message key of the display message
     * @param owner the owner of the action
     */
    public AbstractActionViewableNode(String confirmationMessage, String displayMessage, Node owner)
    {
        this.confirmationMessage = confirmationMessage;
        this.displayMessage = displayMessage;
        this.owner = owner;
    }

    public String getConfirmationMessage()
    {
        return confirmationMessage;
    }

    public String getMimeType()
    {
        return Viewable.RAVEN_ACTION_MIMETYPE;
    }

    public Object getData()
    {
        try
        {
            return executeAction();
        }
        catch (Exception ex)
        {
            if (owner.isLogLevelEnabled(LogLevel.ERROR))
                owner.getLogger().error(String.format("Error executing action (%s)", toString()), ex);
            return actionExecutionErrorMessage+" "+ex.getMessage();
        }
    }

    public boolean cacheData()
    {
        return false;
    }

    public int getWidth()
    {
        return 0;
    }

    public int getHeight()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return displayMessage;
    }

    public Collection<NodeAttribute> getActionAttributes()
    {
        return null;
    }

    public abstract String executeAction() throws Exception;
}
