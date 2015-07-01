/*
 *  Copyright 2008 tim.
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

package org.raven.graph;

import java.awt.Color;


/**
 *
 * @author Mikhail Titov
 */
public enum GraphColor
{
    BLACK(Color.BLACK), BLUE(Color.BLUE), CYAN(Color.CYAN), DARK_GRAY(Color.DARK_GRAY),
    GRAY(Color.GRAY), GREEN(Color.GREEN), LIGHT_GRAY(Color.LIGHT_GRAY), MAGENTA(Color.MAGENTA),
    ORANGE(Color.ORANGE), PINK(Color.PINK), RED(Color.RED),
    WHITE(Color.WHITE), YELLOW(Color.YELLOW);
    
    private Color color;
    
    GraphColor(Color color)
    {
        this.color = color;
    }

    public Color getColor()
    {
        return color;
    }
    
    public GraphColor nextColor()
    {
        if (ordinal()==values().length-1)
            return values()[0];
        else
            return values()[ordinal()+1];
    }
}
