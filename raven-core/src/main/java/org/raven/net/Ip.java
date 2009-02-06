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

package org.raven.net;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Mikhail Titov
 */
public class Ip
{
    private final static Pattern ipPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");
    
    private final int ip;
    private final String ipStr;

    public Ip(int ip)
    {
        this.ip = ip;

        int mask=255;
        StringBuilder strIp = new StringBuilder();
        for (int i=0; i<4; ++i)
        {
            if (i!=0) ip>>>=8;
            strIp.append((i!=0? ".":"")+(ip&mask));
        }
        ipStr = strIp.toString();
    }

    public static Ip parse(String ipStr) throws InvalidIpException
    {
        Ip ip = null;
        Matcher ipMatcher = ipPattern.matcher(ipStr);
        if (!ipMatcher.matches())
        {
            try
            {
                int intIp = Integer.parseInt(ipStr);
                ip = new Ip(intIp);
            }
            catch(NumberFormatException e) {
                throw new InvalidIpException(ipStr);
            }
        }
        else
        {
            int intIp = 0;
            for (int i=4; i>=1; --i)
            {
                try{
                    short octet = Short.parseShort(ipMatcher.group(i));
                    if (octet>255) throw new InvalidIpException(ipStr);
                    if (i!=4) intIp<<=8;
                        intIp|=octet;
                }catch(NumberFormatException e){
                    throw new InvalidIpException(ipStr);
                }
            }
            ip = new Ip(intIp);
        }
        return ip;
    }

    public int getIp()
    {
        return ip;
    }

    @Override
    public String toString()
    {
        return ipStr;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final Ip other = (Ip) obj;
        if (this.ip != other.ip)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 41 * hash + this.ip;
        return hash;
    }
}
